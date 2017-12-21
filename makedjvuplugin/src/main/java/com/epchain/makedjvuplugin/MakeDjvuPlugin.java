package com.epchain.makedjvuplugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Manifest;

public class MakeDjvuPlugin implements Plugin<Project>
{
  public static final String PLUGIN_NAME = MakeDjvuPlugin.class.getSimpleName();

  private SettingsPluginExtension settingsExtension;
  private NamedDomainObjectContainer<Book> booksContainer;

  @Override
  public void apply( Project project )
  {
    printVersion( project );
    setupExtension( project );
    setupBooksContainer( project );
    setupUsageTask( project );
  }

  private void printVersion( Project project )
  {
    try
    {
      URLClassLoader cl = (URLClassLoader) getClass().getClassLoader();
      URL url = cl.findResource( "META-INF/MANIFEST.MF" );
      Manifest manifest = new Manifest( url.openStream() );
      String pluginVersion = manifest.getMainAttributes().getValue( "plugin-version" );
      project.getLogger().lifecycle( PLUGIN_NAME + " v." + pluginVersion );
    }
    catch ( IOException e )
    {
      project.getLogger().warn( "Cannot read plugin's manifest." );
    }
  }

  private void setupExtension( Project project )
  {
    settingsExtension = project.getExtensions().create(
      "settings", SettingsPluginExtension.class, project
    );
  }

  private void setupBooksContainer( Project project )
  {
    booksContainer = project.container( Book.class );
    project.getExtensions().add( "books", booksContainer );
    // Create tasks for every specified book, if possible
    booksContainer.all(
      book ->
        project.afterEvaluate(
          proj ->
          {
            boolean isBookBroken = !Utils.isPdf( book.getBookFile() ) && !Utils.isImageSet( book.getBookFile() );
            boolean skipBook = book.skip();
            boolean cannotProcessPdf = Utils.isPdf(book.getBookFile()) && !ConvertPdfToImagesTask.toolsFound();
            boolean cannotProcessImages = !CreateDjvuFromImagesTask.toolsFound();
            if ( cannotProcessPdf ) skipBook = true;

            if ( !skipBook && !isBookBroken && !cannotProcessImages )
            {
              // Creating tasks
              DefaultTask bookTask =
                project.getTasks().create( "convert" + Utils.capitalize( book.getName() ), DefaultTask.class );
              bookTask.setGroup( PLUGIN_NAME );
              bookTask.setDescription( "Convert '" + book.getName() + "' into DJVU book." );

              CreateDjvuFromImagesTask createDjvuFromImagesTask =
                project.getTasks().create( book.getName() + "ImagesToDjvu", CreateDjvuFromImagesTask.class );
              createDjvuFromImagesTask.setGroup( PLUGIN_NAME );
              createDjvuFromImagesTask.setDescription( "Create DJVU book from '" + book.getName() + "' images." );

              JpgToPngTask jpgToPngTask =
                project.getTasks().create( book.getName() + "JpgToPng", JpgToPngTask.class );
              jpgToPngTask.setGroup( PLUGIN_NAME );
              jpgToPngTask.setDescription(
                "Converts JPG images to PNG for '" + book.getName() + "'. WARNING: task deletes old JPGs"
              );

              ConvertPdfToImagesTask convertPdfToImagesTask;

              AddBookmarksToDjvuTask addBookmarksToDjvuTask;

              // Setting up tasks
              createDjvuFromImagesTask.setWorkDir( settingsExtension.getWorkDirProvider() );
              createDjvuFromImagesTask.setOutDir( settingsExtension.getOutDirProvider() );
              String djvuName = Utils.getFilenameWithoutExtension( book.getBookFile().getName() ) + ".djvu";
              File djvu = ( book.isOutDirValid() )
                ? new File( book.getOutDir(), djvuName )
                : new File( settingsExtension.getOutDir(), djvuName );
              createDjvuFromImagesTask.setDjvu( djvu );
              createDjvuFromImagesTask.setBook( book );
              // Implying that book is an image set. Images dir will be book file.
              createDjvuFromImagesTask.setImagesDir( book.getBookFile() );
              jpgToPngTask.setImagesDir( book.getBookFile() );

              bookTask.dependsOn( createDjvuFromImagesTask );

              if ( Utils.isPdf(book.getBookFile()) && ConvertPdfToImagesTask.toolsFound() )
              {
                convertPdfToImagesTask =
                  project.getTasks().create( book.getName() + "PdfToImages", ConvertPdfToImagesTask.class );
                convertPdfToImagesTask.setGroup( PLUGIN_NAME );
                convertPdfToImagesTask.setDescription(
                  "Convert PDF '" + book.getName() + "' with image pages into set of images."
                );

                File outDir = new File( settingsExtension.getWorkDir(), book.getName() );
                convertPdfToImagesTask.setWorkDir( settingsExtension.getWorkDirProvider() );
                convertPdfToImagesTask.setOutDir( outDir );
                convertPdfToImagesTask.setBook( book );
                // Override previously set images dir with out dir of PDF conversion task
                createDjvuFromImagesTask.setImagesDir( outDir );
                jpgToPngTask.setImagesDir( outDir );

                createDjvuFromImagesTask.dependsOn( convertPdfToImagesTask );
              }

              if ( book.isBookmarksFound() )
              {
                addBookmarksToDjvuTask =
                  project.getTasks().create( book.getName() + "AddBookmarks", AddBookmarksToDjvuTask.class );
                addBookmarksToDjvuTask.setGroup( PLUGIN_NAME );
                addBookmarksToDjvuTask.setDescription( "Add/replace bookmarks for '" + book.getName() + "'." );

                addBookmarksToDjvuTask.setBook( book );
                addBookmarksToDjvuTask.setDjvu( djvu );
              }
            }
            else
            {
              if ( cannotProcessImages )
              {
                project.getLogger().error( "Plugin cannot process images!" );
                project.getLogger().error( "Install DjvuLibre and add installation to PATH environment variable" );
              }
              if ( cannotProcessPdf )
              {
                project.getLogger().error( "'" + book.getName() + "' is PDF, but cannot be processed" );
                project.getLogger().error( "Install Calibre and add installation to PATH environment variable" );
              }
              if ( skipBook )
              {
                project.getLogger().lifecycle( "'" + book.getName() + "' skipped" );
              }
              if ( isBookBroken )
              {
                project.getLogger().error( "'" + book.getName() + "' has wrong block settings/data or unsupported" );
              }
            }
          }
        )
    );
  }

  private void setupUsageTask( Project project )
  {
    project.getTasks().create(
      "usage",
      UsageTask.class,
      usageTask ->
      {
        usageTask.setGroup( PLUGIN_NAME );
        usageTask.setDescription( "Prints plugin usage info." );
      }
    );
  }
}
