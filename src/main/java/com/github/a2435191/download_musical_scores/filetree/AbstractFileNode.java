package com.github.a2435191.download_musical_scores.filetree;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a mutable node in a tree of files to be downloaded from HTTP
 */
public abstract class AbstractFileNode {


    private final @NotNull List<@NotNull AbstractFileNode> children = new ArrayList<>();
    /**
     * Parent node. <code>null</code> if no parent.
     *
     * @see AbstractFileNode#isRoot()
     */
    private @Nullable AbstractFileNode parent;

    /**
     * Download the file if this is a file; otherwise, create a folder.
     *
     * @param parentDir The parent directory absolute downloadDir where downloading/creating should occur, URL-escaped.
     *                  Does <b>not</b> create this directory automatically.
     * @return The absolute downloadDir of where the file/folder was downloaded/created.
     * @throws IOException if the download fails
     * @apiNote This does <b>not</b> take into account the structure of the tree!
     * A FileNode representing a/b/c.txt calling saveToDisk in folder d will write d/c.txt, not d/a/b/c.txt!
     */
    public abstract @NotNull Path saveToDisk(@NotNull Path parentDir) throws IOException;

    /**
     * Helper method to determine if this node represents a directory or file.
     *
     * @return true if this node represents a directory; it represents a file otherwise
     */
    public final boolean isDirectory() {
        return !children.isEmpty();
    }

    /**
     * Helper method to determine if this node is the root node (has no parents).
     *
     * @return true if this node doesn't have a parent, false otherwise.
     */
    public final boolean isRoot() {
        return parent == null;
    }

    /**
     * Gets the children nodes for <code>this</code>.
     *
     * @return a read-only list of children
     */
    @Contract(pure = true)
    public final @NotNull @UnmodifiableView List<? extends AbstractFileNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void setChildren(@NotNull Collection<? extends AbstractFileNode> newChildren) {
        for (@NotNull AbstractFileNode node : this.children) {
            node.parent = null;
        }

        this.children.clear();
        for (@NotNull AbstractFileNode node : newChildren) {
            node.parent = this;
            this.children.add(node);
        }
    }

    public void addChild(@NotNull AbstractFileNode newChild) {
        newChild.parent = this;
        this.children.add(newChild);
    }

    public @Nullable AbstractFileNode getParent() {
        return parent;
    }

    public void setParent(@Nullable AbstractFileNode parent) {
        this.parent = parent;
    }

}