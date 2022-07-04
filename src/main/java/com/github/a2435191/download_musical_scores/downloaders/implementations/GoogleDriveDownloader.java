package com.github.a2435191.download_musical_scores.downloaders.implementations;

import com.github.a2435191.download_musical_scores.downloaders.AbstractDirectLinkFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import com.github.a2435191.download_musical_scores.filetree.URLFileNodeWithKnownName;
import com.github.a2435191.download_musical_scores.reddit.RedditClient;
import com.github.a2435191.download_musical_scores.util.FileUtils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Set;

public final class GoogleDriveDownloader extends AbstractDirectLinkFileDownloader {
    public static final String DEFAULT_CREDENTIALS_PATH = "/gdrive_credentials.json";
    public static final String DEFAULT_TOKENS_PATH = "tokens/";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final Set<String> SCOPES = Set.of(DriveScopes.DRIVE_METADATA_READONLY);
    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
    private static final String SHORTCUT_MIME_TYPE = "application/vnd.google-apps.shortcut";
    private static final int FOLDER_CHILD_PAGE_SIZE = 50;
    private final String credentialsPath;
    private final String tokensDirectoryPath;
    private Drive service;

    public GoogleDriveDownloader(int timeoutSeconds) throws GeneralSecurityException, IOException {
        super(timeoutSeconds);
        this.credentialsPath = DEFAULT_CREDENTIALS_PATH;
        this.tokensDirectoryPath = DEFAULT_TOKENS_PATH;
        this.generateService();
    }


    public GoogleDriveDownloader(int timeoutSeconds, String credentialsPath, String tokensDirectoryPath) throws GeneralSecurityException, IOException {
        super(timeoutSeconds);
        this.credentialsPath = credentialsPath;
        this.tokensDirectoryPath = tokensDirectoryPath;
        this.generateService();
    }

    private static void throwExceptionOnMissingField(
        @NotNull String fieldName,
        @Nullable Object field
    ) throws IOException {
        if (field == null) {
            String errorMsg = "failed to get " + fieldName + " attribute from file!";
            throw new IOException(errorMsg);
        }
    }

    private static @NotNull String extractDriveIdFromURL(@NotNull String url) {
        String path = URI.create(url).getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] urlPaths = path.split("/");
        return urlPaths[2]; // whether /file/d/<id> or /drive/folders/<id>, we index at 2
    }

    private void generateService() throws GeneralSecurityException, IOException {
        final NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        this.service = new Drive.Builder(transport, JSON_FACTORY, getCredentials(transport))
            .setApplicationName(RedditClient.USER_AGENT)
            .build();
    }

    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        InputStream in = GoogleDriveDownloader.class.getResourceAsStream(credentialsPath);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + credentialsPath);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
            .setAccessType("offline")
            .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(-1).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Get the {@link AbstractFileNode} for a {@link File} object representing a folder.
     *
     * @param fileOrFolder {@link File} object. Should represent a file or folder,
     *                     and must always have the fields
     *                     <code>name</code>, <code>id</code>, and <code>mimeType</code>.
     *                     If it represents a file, it must also have <code>webContentLink</code>.
     * @return A {@link AbstractFileNode} object whose children (if any) are files and folders themselves.
     * @throws IOException if any of the required fields are missing.
     */
    private @NotNull AbstractFileNode getNodeForFileOrFolder(@NotNull File fileOrFolder) throws IOException {
        // TODO: verify this works with shared drives
        String name = fileOrFolder.getName();
        String mimeType = fileOrFolder.getMimeType();
        String id = fileOrFolder.getId();

        throwExceptionOnMissingField("name", name);
        throwExceptionOnMissingField("mimeType", mimeType);
        throwExceptionOnMissingField("id", id);

        if (mimeType.equals(FOLDER_MIME_TYPE)) { // folder

            String query = "'" + id + "'" + " in parents and trashed=false";

            AbstractFileNode out = new URLFileNodeWithKnownName(name);

            @Nullable String nextPageToken = null;
            do {
                FileList fileList = service.files().list()
                    .setQ(query)
                    .setPageToken(nextPageToken)
                    .setPageSize(FOLDER_CHILD_PAGE_SIZE)
                    .setFields("nextPageToken,files(id,name,mimeType,webContentLink)")
                    .execute();
                nextPageToken = fileList.getNextPageToken();

                for (File childFile : fileList.getFiles()) {
                    AbstractFileNode childNode = getNodeForFileOrFolder(childFile);
                    out.addChild(childNode);
                }

            } while (nextPageToken != null);
            return out;
        } else if (mimeType.equals(SHORTCUT_MIME_TYPE)) {
            // "shortcutDetails.targetId, targetMimeType"
            File shortcutInfo = this.service.files().get(id)
                .setFields("shortcutDetails(targetId)").execute();
            String targetId = shortcutInfo.getShortcutDetails().getTargetId();

            // TODO: probably should make sure this doesn't infinitely recurse, but eh
            return getFileTreeRootById(targetId);
        } else { // otherwise, file
            String url = fileOrFolder.getWebContentLink();
            throwExceptionOnMissingField("webContentLink", url);

            if (!url.contains("&confirm=t")) { // for large files, a confirmation page comes up without this
                url = url.concat("&confirm=t");
            }

            return new URLFileNodeWithKnownName(name, url);
        }
    }

    /**
     * Get a {@code FileNode} corresponding to a Google Drive file or folder.
     *
     * @param id Google Drive ID of the file/folder. Not a URL.
     * @return A {@code FileNode} corresponding to a Google Drive file or directory structure.
     * @throws IOException if the request cannot be completed.
     *                     Check {@link GoogleJsonResponseException#getStatusCode()} for <code>404</code>.
     * @see GoogleDriveDownloader#getFileTreeRoot(String)
     */
    public @NotNull AbstractFileNode getFileTreeRootById(@NotNull String id) throws IOException {
        // TODO: test
        @NotNull File root;

        try {
            root = service.files().get(id).setFields("id,name,mimeType,webContentLink").execute();
        } catch (TokenResponseException e) {
            final int statusCode = e.getStatusCode();
            System.out.println("HERE: " + statusCode);
            if (statusCode == 400 || statusCode == 401) { // 400/401, try to have user login again by clearing cached tokens
                e.printStackTrace(System.out);
                try {
                    FileUtils.delete(Path.of(tokensDirectoryPath));
                    generateService();
                } catch (GeneralSecurityException generalSecurityException) {
                    throw new IOException(e);
                }
                root = service.files().get(id).setFields("id,name,mimeType,webContentLink").execute();
            } else {
                throw e;
            }
        }

        Objects.requireNonNull(root);

        return getNodeForFileOrFolder(root);
    }

    @Override
    public @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) throws IOException {
        return getFileTreeRootById(extractDriveIdFromURL(url));
    }
}