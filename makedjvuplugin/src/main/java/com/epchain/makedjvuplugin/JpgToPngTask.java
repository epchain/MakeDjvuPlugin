package com.epchain.makedjvuplugin;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class JpgToPngTask extends DefaultTask
{
  private File imagesDir;

  @InputDirectory
  @OutputDirectory
  public File getImagesDir()
  {
    return imagesDir;
  }

  public void setImagesDir( File imagesDir )
  {
    this.imagesDir = imagesDir;
  }

  @TaskAction
  public void jpgToPng()
  {
    getLogger().lifecycle( "Converting JPG files to PNG" );

    File[] files = imagesDir.listFiles();
    int convertedJgps = 0;
    if ( files != null )
    {
      for ( File file : files )
      {
        if ( !Utils.isJpg(file) ) continue;

        File png = new File( imagesDir, Utils.getFilenameWithoutExtension(file.getName()) + ".png" );
        if ( png.exists() && png.isFile() )
        {
          fail( png.getName() + " already exists. Aborting operation." );
        }

        try
        {
          final BufferedImage image = ImageIO.read( file );
          ImageIO.write( image, "png", png );
        }
        catch ( IOException e )
        {
          fail( "Failed to convert " + file.getName() + " to PNG" );
        }

        if ( !png.exists() && !png.isFile() )
        {
          fail( png.getName() + " not exists after conversion process" );
        }

        getLogger().lifecycle( file.getName() + " converted to " + png.getName() );
        if ( file.delete() ) getLogger().lifecycle( file.getName() + " deleted" );

        ++convertedJgps;
      }
    }
    else
    {
      fail( "BUG: Images dir points to file" );
    }

    getLogger().lifecycle( convertedJgps + " files converted" );
  }

  private void fail( String message )
  {
    getLogger().error( message );
    throw new RuntimeException( message );
  }
}