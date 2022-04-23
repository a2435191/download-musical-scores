package com.github.a2435191.download_musical_scores.filetree;

import com.github.a2435191.download_musical_scores.reddit.RedditClient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents a mutable node in a tree of files to be downloaded from HTTP
 */
public abstract class AbstractFileNode {


    public record FileInfo(@NotNull InputStream data, @NotNull String name) {}

    private final @NotNull List<@NotNull AbstractFileNode> children = new ArrayList<>();

    /**
     * Parent node. <code>null</code> if no parent.
     *
     * @see AbstractFileNode#isRoot()
     */
    private @Nullable AbstractFileNode parent;


    public abstract @NotNull FileInfo download(@NotNull Path parentDir) throws IOException;

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