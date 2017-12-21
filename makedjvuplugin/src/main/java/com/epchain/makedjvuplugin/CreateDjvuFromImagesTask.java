package com.epchain.makedjvuplugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class CreateDjvuFromImagesTask extends DefaultTask
{
  private final Property<File> workDir;
  private final Property<File> outDir;
  private Book book;
  private File imagesDir;
  private File bookWorkDir;
  private File djvu;

  public CreateDjvuFromImagesTask()
  {
    workDir = getProject().getObjects().property( File.class );
    outDir = getProject().getObjects().property( File.class );
  }

  @Input
  public File getWorkDir()
  {
    return workDir.get();
  }

  public void setWorkDir( File workDir )
  {
    this.workDir.set( workDir );
  }

  public void setWorkDir( Provider<File> workDir )
  {
    this.workDir.set( workDir );
  }

  @Input
  public File getOutDir()
  {
    return outDir.get();
  }

  public void setOutDir( File outDir )
  {
    this.outDir.set( outDir );
  }

  public void setOutDir( Provider<File> outDir )
  {
    this.outDir.set( outDir );
  }

  @Input
  public Book getBook()
  {
    return book;
  }

  public void setBook( Book book )
  {
    this.book = book;
  }

  @InputDirectory
  public File getImagesDir()
  {
    return imagesDir;
  }

  public void setImagesDir( File imagesDir )
  {
    this.imagesDir = imagesDir;
  }

  @OutputFile
  public File getDjvu()
  {
    return djvu;
  }

  public void setDjvu( File djvu )
  {
    this.djvu = djvu;
  }

  @TaskAction
  public void createDjvuFromImages()
  {
    getLogger().lifecycle( "Beginning '" + book.getName() + "' conversion from image set to DJVU." );
    checkTools();
    checkBook();
    if ( !checkDjvu() ) return;
    checkWorkDir();
    checkBookWorkDir();
    List<File> djvuPages = convertEachImageToDjvu();
    createMultipageDjvu( djvuPages );
  }

  public static boolean toolsFound()
  {
    boolean allFound = Utils.executablePath( "c44" ) != null;
    allFound &= Utils.executablePath( "djvm" ) != null;
    return allFound;
  }

  private void checkTools()
  {
    String c44Path = Utils.executablePath( "c44" );
    String djvmPath = Utils.executablePath( "djvm" );

    String recommendation = "Add DjvuLibre installation directory to PATH environment variable.";
    if ( c44Path == null )
    {
      getLogger().error( recommendation );
      fail( "DjvuLibre c44 utility not found" );
    }
    if ( djvmPath == null )
    {
      getLogger().error( recommendation );
      fail( "DjvuLibre djvm utility not found" );
    }
    getLogger().lifecycle( "DjvuLibre c44 found at: " + c44Path );
    getLogger().lifecycle( "DjvuLibre djvm found at: " + djvmPath );
  }

  private void checkBook()
  {
    getLogger().lifecycle( "Checking book..." );
    getLogger().lifecycle( "Book file: " + book.getBookFile().getAbsolutePath() );
    if ( !Utils.isImageSet(book.getBookFile()) )
    {
      getLogger().lifecycle( "'" + book.getName() + "' book file is not an image set. Checking images dir..." );
      getLogger().lifecycle( "Images dir: " + imagesDir.getAbsolutePath() );
      if ( !Utils.isImageSet(imagesDir) ) fail( "'" + book.getName() + "' is not a valid image set" );
    }
    getLogger().lifecycle( "'" + book.getName() + "' seems to be an image set" );
  }

  private boolean checkDjvu()
  {
    if ( djvu.exists() )
    {
      if ( !book.forceCleanUp() )
      {
        getLogger().error( "Found destination DJVU: " + djvu.getAbsolutePath() );
        getLogger().error( "You may accidentally overwrite this file." );
        getLogger().error( "You must set 'forceCleanUp = true' in '" + book.getName() + "' block" );
        getLogger().error( "or manually delete DJVU file." );
        getLogger().error( "Task will not proceed." );
        return false;
      }
      else
      {
        if ( !djvu.delete() )
        {
          fail( "Failed to delete " + djvu.getName() );
        }
        getLogger().lifecycle( "Deleted DJVU: " + djvu.getName() );
      }
    }

    return true;
  }

  private void checkWorkDir()
  {
    getLogger().lifecycle( "Checking work dir..." );
    switch ( Utils.createDirIfNotExists(getWorkDir()) )
    {
      case EXISTS:
        getLogger().lifecycle( "Work dir found: " + getWorkDir().getAbsolutePath() );
        break;

      case CREATED:
        getLogger().lifecycle( "Work dir created: " + getWorkDir().getAbsolutePath() );
        break;

      default: fail( "Work dir not found nor created" );
    }
  }

  private void checkBookWorkDir()
  {
    getLogger().lifecycle( "Checking '" + book.getName() + "' work dir..." );

    bookWorkDir = new File( getWorkDir(), book.getName() );
    switch ( Utils.createDirIfNotExists(bookWorkDir) )
    {
      case EXISTS:
        getLogger().lifecycle( "Work dir found: " + bookWorkDir.getAbsolutePath() );
        break;

      case CREATED:
        getLogger().lifecycle( "Work dir created: " + bookWorkDir.getAbsolutePath() );
        break;

      default: fail( "'" + book.getName() + "' work dir not found nor created" );
    }
  }

  private List<File> convertEachImageToDjvu()
  {
    getLogger().lifecycle( "Converting each image into single page DJVU..." );

    List<File> djvuPages = new LinkedList<>();

    File[] files = imagesDir.listFiles();
    if ( files != null )
    {
      for ( File file : files )
      {
        boolean fileSupported = Utils.isJpg(file) || Utils.isPng(file);
        if ( !fileSupported ) continue;

        File djvuPage = new File( bookWorkDir, Utils.getFilenameWithoutExtension(file.getName()) + ".djvu" );
        File tmpPortableMap = new File( bookWorkDir, Utils.getFilenameWithoutExtension(file.getName()) + ".pm" );

        String portableMapExt = Utils.imageToPortableMap( file, tmpPortableMap, MakeDjvuPlugin.PLUGIN_NAME );
        if ( portableMapExt == null )
        {
          fail( "Failed to convert " + file.getName() + ": unsupported format." );
        }

        String correctPmName = Utils.getFilenameWithoutExtension( tmpPortableMap.getName() ) + "." + portableMapExt;
        File portableMap = new File( bookWorkDir, correctPmName );
        if ( !tmpPortableMap.renameTo(portableMap) )
        {
          fail( "Failed to rename " + tmpPortableMap.getName() + " to " + portableMap.getName() );
        }

        try
        {
          ProcessBuilder processBuilder = getProcessBuilderFor( portableMap, djvuPage );
          if ( processBuilder == null )
          {
            fail( "Failed to create process for " + portableMap.getName() );
          }
          else
          {
            Process process = processBuilder.start();
            process.waitFor();
          }
        }
        catch ( IOException | InterruptedException e )
        {
          fail( "Failed to convert " + file.getName() );
        }

        if ( !djvuPage.exists() && !djvuPage.isFile() )
        {
          fail( djvuPage.getName() + " not exists after conversion process" );
        }

        djvuPages.add( djvuPage );

        // Portable maps are huge and unnecessary, so they are deleted
        if ( !portableMap.delete() )
        {
          fail( "Failed to delete " + portableMap.getName() );
        }

        getLogger().lifecycle( file.getName() + " converted to " + djvuPage.getName() );
      }
    }
    else
    {
      fail( "BUG: Images dir points to file" );
    }

    getLogger().lifecycle( djvuPages.size() + " images converted" );
    return djvuPages;
  }

  private void createMultipageDjvu( List<File> djvuPages )
  {
    if ( djvuPages.size() < 1 ) return;
    getLogger().lifecycle( "Creating multipage DJVU from single DJVU pages..." );

    // Create book from first page
    File firstPage = djvuPages.get( 0 );
    ProcessBuilder processBuilder = new ProcessBuilder(
      "djvm",
      "-c", // create
      "\"" + djvu.getAbsolutePath() + "\"",
      "\"" + firstPage.getAbsolutePath() + "\""
    );

    try
    {
      Process process = processBuilder.start();
      process.waitFor();
    }
    catch ( IOException | InterruptedException e )
    {
      fail( "Failed to create " + djvu.getName() + " from " + firstPage.getName() );
    }
    // Now skip first page
    djvuPages.remove( 0 );

    getLogger().lifecycle( "Created book with first page" );

    if ( !book.keepIntermediates() )
    {
      if ( firstPage.delete() ) getLogger().lifecycle( "Deleted " + firstPage.getName() );
    }

    // Append rest pages to main book file
    for ( File djvuPage : djvuPages )
    {
      processBuilder = new ProcessBuilder(
        "djvm",
        "-i", // insert
        "\"" + djvu.getAbsolutePath() + "\"",
        "\"" + djvuPage.getAbsolutePath() + "\""
      );

      try
      {
        Process process = processBuilder.start();
        process.waitFor();
      }
      catch ( IOException | InterruptedException e )
      {
        fail( "Failed to append " + djvuPage.getName() + " to " + djvu.getName() );
      }

      getLogger().lifecycle( djvuPage.getName() + " appended to " + djvu.getName() );

      if ( !book.keepIntermediates() )
      {
        if ( djvuPage.delete() ) getLogger().lifecycle( "Deleted " + djvuPage.getName() );
      }
    }
  }

  private ProcessBuilder getProcessBuilderFor( File portableMap, File outputDjvu )
  {
    switch ( Utils.getFileExtension(portableMap.getName()) )
    {
      case "ppm":
      case "pgm":
        return new ProcessBuilder(
          "c44",
          "-decibel", "48", // max quality
          "\"" + portableMap.getAbsolutePath() + "\"",
          "\"" + outputDjvu.getAbsolutePath() + "\""
        );

      default: return null;
    }
  }

  private void fail( String message )
  {
    getLogger().error( message );
    throw new RuntimeException( message );
  }
}