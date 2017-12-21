package com.epchain.makedjvuplugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class AddBookmarksToDjvuTask extends DefaultTask
{
  private Book book;
  private File djvu;

  @Input
  public Book getBook()
  {
    return book;
  }

  public void setBook( Book book )
  {
    this.book = book;
  }

  @Input
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
  public void addBookmarksToDjvu()
  {
    getLogger().lifecycle( "Trying to add/replace bookmarks for '" + book.getName() + "'" );
    checkBook();
    checkTools();
    addBodokmarksToDjvu();
  }

  private void checkBook()
  {
    if ( Utils.isDjvu(djvu) )
    {
      getLogger().lifecycle( "DJVU file found: " + djvu.getAbsolutePath() );
    }
    else fail( "DJVU file not found" );

    if ( book.isBookmarksFound() )
    {
      getLogger().lifecycle( "Bookmarks file found: " + book.getBookmarksFile().getAbsolutePath() );
    }
    else fail( "Bookmarks file not found" );
  }

  private void checkTools()
  {
    String djvusedPath = Utils.executablePath( "djvused" );
    if ( djvusedPath == null )
    {
      getLogger().error( "Add DjvuLibre installation directory to PATH environment variable." );
      fail( "DjvuLibre djvused utility not found" );
    }
    getLogger().lifecycle( "DjvuLibre djvused found at: " + djvusedPath );
  }

  private void addBodokmarksToDjvu()
  {
    // Create book from first page
    ProcessBuilder processBuilder = new ProcessBuilder(
      "djvused",
      "-e", // execute command
      "\"set-outline", book.getBookmarksFile().getAbsolutePath() + "\"",
      "-s", // save executed command result
      "\"" + djvu.getAbsolutePath() + "\""
    );

    try
    {
      Process process = processBuilder.start();
      BufferedReader in = new BufferedReader( new InputStreamReader(process.getErrorStream()) );
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
      fail( "Failed to add/replace bookmarks to " + djvu.getName() );
    }

    getLogger().lifecycle( "Completed" );
  }

  private void fail( String message )
  {
    getLogger().error( message );
    throw new RuntimeException( message );
  }
}
