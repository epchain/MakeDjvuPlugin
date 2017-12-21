package com.epchain.makedjvuplugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class UsageTask extends DefaultTask
{
  @TaskAction
  public void usage()
  {
    getLogger().lifecycle( "See README.md in plugin sources for detailed description." );
    getLogger().lifecycle( "See example project in plugin sources." );
    getLogger().lifecycle( "Books:" );
    getLogger().lifecycle( "  books {" );
    getLogger().lifecycle( "    <book-name> {" );
    getLogger().lifecycle( "      // Directory with images or PDF file" );
    getLogger().lifecycle( "      bookFile = <file>" );
    getLogger().lifecycle( "      // Bookmarks file. Optional" );
    getLogger().lifecycle( "      bookmarksFile = <file>" );
    getLogger().lifecycle( "      // Override output directory for DJVU. Optional" );
    getLogger().lifecycle( "      outDir = <file>" );
    getLogger().lifecycle( "      // Do not process this book" );
    getLogger().lifecycle( "      skip = <true|false>" );
    getLogger().lifecycle( "      // Keep intermediate files. Optional" );
    getLogger().lifecycle( "      keepIntermediates = <true|false>" );
    getLogger().lifecycle( "      // Forces cleanup of images and DJVU. Optional" );
    getLogger().lifecycle( "      forceCleanUp = <true|false>   " );
    getLogger().lifecycle( "    }" );
    getLogger().lifecycle( "    <book-name> {" );
    getLogger().lifecycle( "      ..." );
    getLogger().lifecycle( "    }" );
    getLogger().lifecycle( "    ..." );
    getLogger().lifecycle( "  }" );
    getLogger().lifecycle( "Settings (optional):" );
    getLogger().lifecycle( "  settings {" );
    getLogger().lifecycle( "    workDir = <file> // Directory for intermediate files" );
    getLogger().lifecycle( "    outDir = <file>  // Output directory for DJVU" );
    getLogger().lifecycle( "  }" );
  }
}
