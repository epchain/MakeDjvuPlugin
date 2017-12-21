package com.epchain.makedjvuplugin;

import java.io.File;
import java.io.Serializable;

/** A book settings object. */
public class Book implements Serializable
{
  private static final long serialVersionUID = 1L;

  /** Book block name from Gradle script. */
  private final String name;
  /** Book file or directory with image pages. */
  private File bookFile;
  /** Optional file with DJVU bookmarks. */
  private File bookmarksFile;
  /** Output DJVU book directory. If null, it will be overridden */
  private File outDir;
  /** Skip book conversion. */
  private boolean skip = false;
  /**
   * Forces {@link ConvertPdfToImagesTask} to cleanup book working directory before it starts.
   * {@link ConvertPdfToImagesTask} requires empty book working directory, because it
   * potentially may overwrite user changes to existing image pages.
   */
  private boolean forceCleanUp = false;
  /** Keep intermediates, except for large portable maps. */
  private boolean keepIntermediates = false;

  public Book( String name )
  {
    this.name = name;
  }

  public String getName()
  {
    return name;
  }

  public File getBookFile()
  {
    return bookFile;
  }

  public void setBookFile( File bookFile )
  {
    this.bookFile = bookFile;
  }

  public File getBookmarksFile()
  {
    return bookmarksFile;
  }

  public void setBookmarksFile( File bookmarksFile )
  {
    this.bookmarksFile = bookmarksFile;
  }

  public File getOutDir()
  {
    return outDir;
  }

  public void setOutDir( File outDir )
  {
    this.outDir = outDir;
  }

  public boolean skip()
  {
    return skip;
  }

  public void setSkip( boolean skip )
  {
    this.skip = skip;
  }

  public boolean forceCleanUp()
  {
    return forceCleanUp;
  }

  public void setForceCleanUp( boolean forceCleanUp )
  {
    this.forceCleanUp = forceCleanUp;
  }

  public boolean keepIntermediates()
  {
    return keepIntermediates;
  }

  public void setKeepIntermediates( boolean keepIntermediates )
  {
    this.keepIntermediates = keepIntermediates;
  }

  public boolean isBookmarksFound()
  {
    return bookmarksFile != null && bookmarksFile.exists() && bookmarksFile.isFile();
  }

  public boolean isOutDirValid()
  {
    return outDir != null && outDir.exists() && outDir.isDirectory();
  }
}
