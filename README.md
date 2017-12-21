# MakeDjvuPlugin

A weird Gradle plugin for making DJVU files from images or PDF.
Plugin makes use of DjvuLibre and Calibre software.

## What is this?

This happens to be a Gradle plugin, that converts a set of
JPG or PNG images or PDF into a single DJVU file.
Plugin requires DjvuLibre installation.
PDF support is optional and requires Calibre installation.

Supported image bits-per-pixel values are 8, 24 and 32 as of current needs.
If you use PNGs with alpha channel, colors will be blended with white background.

Typical workflow would be creating Gradle project for single or multiple books.
Plugin provides a separate task for each possible processing operation.
If your book is an image set, you edit files as needed,
then plugin converts images into DJVU straight away.
If book is PDF file, plugin converts PDF into EPUB and extracts images following specific pattern.
Then you can edit intermediate images and create DJVU from resulting image set.
Alternatively, you may wish to create image set book from intermediate images.

You can optionally provide bookmarks file to include in book.
You need manually execute bookmarks task to add/update bookmarks.

There is also optional task for converting JPG files into PNG
for sake of editing pages and not losing quality / wasting disk space.

By default, plugin behaves defensively with your files,
considering you might have edited them.
It will not overwrite images extracted from PDF and output DJVU,
until you manually delete them or enforce cleanup for a book.

## Installation

You may use your existing Gradle installation (version 4.3.1 or higher) or use Gradle Wrapper.
Instructions are given for wrapper. You also need JDK 8 installation.

  * Optionally, specify MAVEN_REPO environment variable.
    If not specified, plugin will be published into **repo** directory after build.
  * Navigate to plugin directory.
  * Execute build and publish plugin artifact:
    ```
    gradlew uploadArchives
    ```
  * Add *DjvuLibre* and, optionally, *Calibre* installation to **PATH** environment variable.

## Usage

Simple book project can be found in **example** directory.
To use plugin  follow theese steps:

  * Create Gradle project (new directory with empty *build.gradle* file)
  * Apply plugin:
    ```
    buildscript {
      repositories {
        maven {
          url uri( System.getenv( "MAVEN_REPO" ) )
        }
      }

      dependencies {
        classpath 'com.epchain:makedjvuplugin:+'
      }
    }
    
    apply plugin: 'makedjvuplugin'
    ```
    Alternatively, maven repo may be custom directory:
    ```
    url uri( "path/to/your/repo" )
    ```
  * Specify books:
    ```
    books {
      pngbook {
        bookFile = file( "books/pngbook/pngbook.pdf" ) // Directory with images or PDF file
        bookmarksFile = file( "books/pngbook/bookmarks.lsp" ) // Bookmarks file. Optional
        outDir = file( "books/pngbook" ) // Override output directory for DJVU. Optional
        skip = false // Do not process this book
        keepIntermediates = true // Keep intermediate files. Optional
        forceCleanup = true // Forces cleanup of images and DJVU. Optional
      }
    }
    ```
  * Specify settings (optional):
    ```
    settings {
      workDir = file( 'work' )      // Directory for intermediate files
      outDir = file( 'converted' )  // Output directory for DJVU
    }
    ```
  * View available tasks:
    ```
    gradlew tasks
    ```
  * If you properly specified book settings, you will see a number of tasks, such as:
    ```
    usage
    convertPngbook
    pngbookPdfToImages
    pngbookImagesToDjvu
    pngbookJpgToPng
    pngbookAddBookmarks
    ```
    Certain tasks may not appear, if requirements are not met.
    For example, there will be no *pngbookPdfToImages* task,
    if there is no PDF file or Calibre tools not found.
  * Execute tasks you like with:
    ```
    gradlew <task-name>
    ```
    *usage* task shows usage info.
    *convertPngbook* task executes *pngbookPdfToImages*, *pngbookImagesToDjvu* and *pngbookAddBookmarks*.

## Credits

[Gradle](https://gradle.org/)  
[DjvuLibre](http://djvu.sourceforge.net/)  
[Calibre](https://calibre-ebook.com/)  

## License

This project is licensed under *The Unlicense*.
See the [LICENSE](LICENSE) file for details.