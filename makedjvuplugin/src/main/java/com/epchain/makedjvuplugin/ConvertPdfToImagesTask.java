package com.epchain.makedjvuplugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ConvertPdfToImagesTask extends DefaultTask
{
  private final Property<File> workDir;
  private Book book;
  private File outDir;
  private File epub;

  public ConvertPdfToImagesTask()
  {
    workDir = getProject().getObjects().property( File.class );
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
  public Book getBook()
  {
    return book;
  }

  public void setBook( Book book )
  {
    this.book = book;
  }

  @OutputDirectory
  public File getOutDir()
  {
    return outDir;
  }

  public void setOutDir( File outDir )
  {
    this.outDir = outDir;
  }

  @TaskAction
  public void convertPdfToImageFiles()
  {
    getLogger().lifecycle( "Beginning '" + book.getName() + "' conversion from PDF to image set." );
    checkTools();
    checkWorkDir();
    if ( !checkOutDir() ) return;
    checkBook();
    getLogger().lifecycle( "Output dir: " + outDir.getAbsolutePath() );
    convertPdfToEpub();
    int indexLength = extractImagesFromEpub();
    if ( !book.keepIntermediates() )
    {
      if ( epub.delete() ) getLogger().lifecycle( "Deleted " + epub.getName() );
    }
    renameImages( indexLength );
  }

  public static boolean toolsFound()
  {
    return Utils.executablePath( "ebook-convert" ) != null;
  }

  private void checkTools()
  {
    String ebookConvertPath = Utils.executablePath( "ebook-convert" );
    if ( ebookConvertPath == null )
    {
      getLogger().error( "Add Calibre installation directory to PATH environment variable." );
      fail( "Calibre ebook-convert utility not found." );
    }
    getLogger().lifecycle( "Calibre ebook-convert found at: " + ebookConvertPath );
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

  private boolean checkOutDir()
  {
    File[] files = getOutDir().listFiles();
    if ( files == null )
    {
      fail( "Out dir points to file" );
      return false;
    }
    if ( (files.length > 0) )
    {
      if ( book.forceCleanUp() )
      {
        getLogger().lifecycle( "Cleaning up book working directory..." );
        boolean allDeleted = true;
        for ( File file : files )
        {
          allDeleted &= file.delete();
        }

        if ( !allDeleted )
        {
          fail( "Failed to clean up book working directory files" );
        }
        else
        {
          getLogger().lifecycle( "Clean up completed" );
        }
      }
      else
      {
        getLogger().error( "Found files inside book working directory: " + getOutDir().getAbsolutePath() );
        getLogger().error( "This may lead to accidental overwriting of changes you made to images" );
        getLogger().error( "You must set 'forceCleanUp = true' in '" + book.getName() + "' block" );
        getLogger().error( "or manually delete all files from '" + book.getName() + "' working directory" );
        getLogger().error( "Task will not proceed" );
        return false;
      }
    }

    return true;
  }

  private void checkBook()
  {
    getLogger().lifecycle( "Checking book..." );
    if ( !Utils.isPdf( book.getBookFile() ) ) fail( "'" + book.getName() + "' is not a valid PDF book" );
    getLogger().lifecycle( "'" + book.getName() + "' seems to be PDF" );
    getLogger().lifecycle( "Book file: " + book.getBookFile().getAbsolutePath() );
  }

  private void convertPdfToEpub()
  {
    getLogger().lifecycle( "Generating EPUB from " + book.getBookFile().getName() + "..." );

    String epubName = Utils.getFilenameWithoutExtension( book.getBookFile().getName() ) + ".epub";
    epub = new File( outDir, epubName );

    ProcessBuilder processBuilder = new ProcessBuilder(
      "ebook-convert",
      "\"" + book.getBookFile().getAbsolutePath() + "\"",
      "\"" + epub.getAbsolutePath() + "\"",
      "--input-profile", "default",
      "--output-profile", "tablet"
    );

    try
    {
      Process process = processBuilder.start();
      BufferedReader in = new BufferedReader( new InputStreamReader(process.getInputStream()) );
      String line;
      while ( (line = in.readLine()) != null )
      {
        getLogger().lifecycle( line );
      }
      process.waitFor();
      in.close();
    }
    catch ( IOException | InterruptedException e )
    {
      fail( "Failed to run ebook-convert" );
    }

    getLogger().lifecycle( "EPUB successfully generated: " + epub.getAbsolutePath() );
  }

  private int extractImagesFromEpub()
  {
    int maxIndexLength = -1;
    int filesExtracted = 0;
    try
    {
      getLogger().lifecycle( "Extracting image files..." );

      FileInputStream epubIn = new FileInputStream( epub );
      ZipInputStream zipIn = new ZipInputStream( new BufferedInputStream(epubIn) );

      ZipEntry entry;
      while( (entry = zipIn.getNextEntry()) != null )
      {
        if ( !entry.isDirectory() && isTargetImage(entry.getName()) )
        {
          // Saving index for further pages renaming
          String strIndex = getStrIndex( entry.getName() );
          maxIndexLength = Math.max( maxIndexLength, strIndex.length() );

          // Extracting image file
          int bufferSize = 2048;
          File extractedImage = new File( outDir, entry.getName() );
          BufferedOutputStream fileOut = new BufferedOutputStream( new FileOutputStream(extractedImage), bufferSize );

          byte[] buffer = new byte[ bufferSize ];
          int readBytes;
          while ( (readBytes = zipIn.read(buffer)) > 0 )
          {
            fileOut.write( buffer, 0, readBytes );
          }

          fileOut.flush();
          fileOut.close();

          getLogger().lifecycle( "Extracted file: " + extractedImage.getName() );
          ++filesExtracted;
        }
      }

      zipIn.closeEntry();
      zipIn.close();
    }
    catch ( IOException e )
    {
      fail( "Failed to extract images from EPUB" );
    }

    getLogger().lifecycle( "Extracted " + filesExtracted + " images" );
    return maxIndexLength;
  }

  private void renameImages( final int indexLength )
  {
    getLogger().lifecycle( "Renaming image files..." );
    getLogger().lifecycle( "Maximum index length of files: " + indexLength );

    int filesRenamed = 0;
    File[] images = outDir.listFiles();
    if ( images != null )
    {
      for ( File image : images )
      {
        String fileName = image.getName();
        if ( isTargetImage(fileName) )
        {
          String strIndex = getStrIndex( fileName );
          StringBuilder newStrIndex = new StringBuilder( indexLength );
          // Building new index with preceding zeroes
          for ( int i = 0; i < (indexLength - strIndex.length()); ++i )
          {
            newStrIndex.append( '0' );
          }
          newStrIndex.append( strIndex );

          String newFilename = "page-" + newStrIndex + "." + Utils.getFileExtension( fileName );
          File renamedFile = new File(image.getParentFile(), newFilename);
          if ( image.renameTo(renamedFile) )
          {
            getLogger().lifecycle( "Renamed " + fileName + " to " + renamedFile.getName() );
            ++filesRenamed;
          }
        }
      }
    }
    else
    {
      fail( "Failed to get image file list for renaming" );
    }

    getLogger().lifecycle( "Files renamed: " + filesRenamed );
  }

  private void fail( String message )
  {
    getLogger().error( message );
    throw new RuntimeException( message );
  }

  private boolean isTargetImage( String fileName )
  {
    Matcher matcher = Pattern.compile( "index-[0-9]+_[0-9]+" ).matcher( fileName );
    return matcher.find();
  }

  private String getStrIndex( String fileName )
  {
    int startIndex = "index-".length();
    int endIndex = fileName.indexOf( "_" );
    return fileName.substring( startIndex, endIndex );
  }
}
