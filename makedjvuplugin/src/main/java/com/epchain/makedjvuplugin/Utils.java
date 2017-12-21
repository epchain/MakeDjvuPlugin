package com.epchain.makedjvuplugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;

public final class Utils
{
  /** Capitalizes a string. */
  public static String capitalize( String str )
  {
    return str.substring( 0, 1 ).toUpperCase() + str.substring( 1 );
  }

  /** Search for executable path with OS-dependent utility. */
  public static String executablePath( String executableName )
  {
    String searchingExecutable =
      System.getProperty( "os.name" ).toLowerCase().contains( "windows" ) ? "where" : "which";

    ProcessBuilder processBuilder = new ProcessBuilder( searchingExecutable, executableName );
    String result;
    try
    {
      Process process = processBuilder.start();
      BufferedReader in = new BufferedReader( new InputStreamReader(process.getInputStream()) );
      result = in.readLine();
      process.waitFor();
      in.close();
    }
    catch ( IOException | InterruptedException e )
    {
      throw new RuntimeException( e );
    }

    return result;
  }

  /**
   * Returns filename without extension.
   * If name has no extension, this name is returned.
   */
  public static String getFilenameWithoutExtension( String fileName )
  {
    int lastDot = fileName.lastIndexOf( '.' );
    return ( lastDot > 0 ) ? fileName.substring( 0, lastDot ) : fileName;
  }

  /** Returns file extension or null, if no extension found.
   */
  public static String getFileExtension( String fileName )
  {
    int lastDot = fileName.lastIndexOf( '.' );
    return ( lastDot > 0 ) ? fileName.substring( lastDot + 1, fileName.length() ) : null;
  }

  public enum DirState { EXISTS, CREATED, FAIL }

  /** Attempts to create dir with all parent dirs if it doesn't exist. */
  public static DirState createDirIfNotExists( File dir )
  {
    if ( dir.exists() && dir.isDirectory() ) return DirState.EXISTS;
    else if ( dir.mkdirs() ) return DirState.CREATED;
    else return DirState.FAIL;
  }

  /** Returns true, if file is supported image set. */
  public static boolean isImageSet( File file )
  {
    if ( file == null ) return false;

    File[] files = file.listFiles();
    if ( files != null )
    {
      int imageCount = 0;
      for ( File f : files )
      {
        if ( isJpg(f) ) ++imageCount;
        else if ( isPng(f) ) ++imageCount;
      }

      return imageCount > 0;
    }
    else
    { // This also means files is file, not directory
      return false;
    }
  }

  /** Returns true, if file is not null, exists and is file, not directory. */
  public static boolean isFileValid( File file )
  {
    return file != null && file.exists() && file.isFile();
  }

  /** Returns true, if file is JPG. */
  public static boolean isJpg( File file )
  {
    return isFileValid( file ) && file.getName().toLowerCase().endsWith( ".jpg" );
  }

  /** Returns true, if file is PNG. */
  public static boolean isPng( File file )
  {
    return isFileValid( file ) && file.getName().toLowerCase().endsWith( ".png" );
  }

  /** Returns true, if file is PDF. */
  public static boolean isPdf( File file )
  {
    return isFileValid( file ) && file.getName().toLowerCase().endsWith( ".pdf" );
  }

  /** Returns true, if file is DJVU. */
  public static boolean isDjvu( File file )
  {
    return isFileValid( file ) && file.getName().toLowerCase().endsWith( ".djvu" );
  }

  /**
   * Converts image file into few supported Portable Image formats using ImageIO.
   * Colored images converted into PPM, grayscale images converted to PGM.
   * @param image a valid ImageIO file
   * @param ppm a non-null file object
   * @param comment comment to add into PPM file
   * @return output file format, if operation succeeded; {@code null} otherwise
   */
  public static String imageToPortableMap( final File image, final File ppm, final String comment )
  {
    String fileFormat;
    try
    {
      final BufferedImage rawImage = ImageIO.read( image );
      int type = rawImage.getType();

      switch ( type )
      { // Check supported types
        case BufferedImage.TYPE_3BYTE_BGR:
        case BufferedImage.TYPE_4BYTE_ABGR:
        case BufferedImage.TYPE_BYTE_GRAY:
          break;

        default: return null;
      }

      BufferedOutputStream ppmOut = new BufferedOutputStream(
        new FileOutputStream( ppm ), 2048
      );

      final int width = rawImage.getWidth();
      final int height = rawImage.getHeight();
      String columnsRows = width + " " + height + "\n";
      final byte[] pixelBuf = ((DataBufferByte) rawImage.getRaster().getDataBuffer()).getData();

      switch ( type )
      {
        case BufferedImage.TYPE_3BYTE_BGR:
        case BufferedImage.TYPE_4BYTE_ABGR:
          ppmOut.write( "P6\n".getBytes() ); // Header
          ppmOut.write( ("# " + comment + "\n").getBytes() ); // Comment
          ppmOut.write( columnsRows.getBytes() ); // Columns & rows
          ppmOut.write( "255\n".getBytes() ); // Maximum color (Maxval)
          // Pixels
          final boolean hasAlpha = rawImage.getAlphaRaster() != null;
          final int colorOffset = (hasAlpha) ? 1 : 0;
          final int bytesPerPixel = 3 + colorOffset;
          final int pixelNum = width * height * bytesPerPixel;
          for ( int pixelIndex = 0; pixelIndex < pixelNum; pixelIndex += bytesPerPixel )
          {
            byte red   = pixelBuf[ pixelIndex + colorOffset + 2 ];
            byte green = pixelBuf[ pixelIndex + colorOffset + 1 ];
            byte blue  = pixelBuf[ pixelIndex + colorOffset ];

            if ( hasAlpha )
            { // Perform alpha blending
              byte alpha = pixelBuf[ pixelIndex ];
              // Using white background, which is common for books
              red   = Utils.alphaBlend( red,   alpha, (byte) 255 );
              green = Utils.alphaBlend( green, alpha, (byte) 255 );
              blue  = Utils.alphaBlend( blue,  alpha, (byte) 255 );
            }

            ppmOut.write( red );
            ppmOut.write( green );
            ppmOut.write( blue );
          }
          fileFormat = "ppm";
          break;

        case BufferedImage.TYPE_BYTE_GRAY:
          ppmOut.write( "P5\n".getBytes() ); // Header
          ppmOut.write( ("# " + comment + "\n").getBytes() ); // Comment
          ppmOut.write( columnsRows.getBytes() ); // Columns & rows
          ppmOut.write( "255\n".getBytes() ); // Maximum color (Maxval)
          // Write as is
          ppmOut.write( pixelBuf );
          fileFormat = "pgm";
          break;

        default:
          throw new IllegalStateException( "BUG: Unexpected format" );
      }

      ppmOut.flush();
      ppmOut.close();
    }
    catch ( IOException e )
    {
      return null;
    }

    return fileFormat;
  }

  /**
   * Performs common blending operation between source color and background color.
   * @param srcColor source color
   * @param srcAlpha source alpha
   * @param bgColor background color
   * @return blended color
   */
  public static byte alphaBlend( final byte srcColor, final byte srcAlpha, final byte bgColor )
  {
    // Raise byte to int
    final int intColor = srcColor & 0xFF;
    final int intBgColor = bgColor & 0xFF;
    final int intAlpha = srcAlpha & 0xFF;
    final int intResult = ((intAlpha * intColor) + ((255 - intAlpha) * intBgColor)) / 255;
    return (byte) ( intResult );
  }
}
