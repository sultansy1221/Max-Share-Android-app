package com.portablefileserver.server

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafePathResolverTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun resolvesRootAndRegularDescendants() {
        val root = temporaryFolder.newFolder("share")
        val nested = File(root, "photos").apply { mkdir() }
        val resolver = SafePathResolver(root)

        assertEquals(root.canonicalFile, resolver.resolve(""))
        assertEquals(nested.canonicalFile, resolver.resolve("photos"))
    }

    @Test
    fun rejectsTraversalAndHiddenPaths() {
        val root = temporaryFolder.newFolder("share")
        val hidden = File(root, ".private").apply { mkdir() }
        File(hidden, "secret.txt").writeText("secret")
        val resolver = SafePathResolver(root)

        assertNull(resolver.resolve("../outside.txt"))
        assertNull(resolver.resolve("folder/../../outside.txt"))
        assertNull(resolver.resolve(".private"))
        assertNull(resolver.resolve(".private/secret.txt"))
    }

    @Test
    fun rejectsSymbolicLinksToExternalAndInternalTargets() {
        val root = temporaryFolder.newFolder("share")
        val inside = File(root, "inside").apply { mkdir() }
        val internalFile = File(inside, "file.txt").apply { writeText("safe") }
        val outsideFile = temporaryFolder.newFile("outside.txt")

        Files.createSymbolicLink(File(root, "internal-link").toPath(), inside.toPath())
        Files.createSymbolicLink(File(root, "external-link").toPath(), outsideFile.toPath())

        val resolver = SafePathResolver(root)
        assertNull(resolver.resolve("internal-link"))
        assertNull(resolver.resolve("internal-link/file.txt"))
        assertNull(resolver.resolve("external-link"))
        assertNull(resolver.resolve("external-link/secret.txt"))
        assertNotNull(resolver.resolve("inside/file.txt"))
        assertEquals(internalFile.canonicalFile, resolver.resolve("inside/file.txt"))
    }
}
