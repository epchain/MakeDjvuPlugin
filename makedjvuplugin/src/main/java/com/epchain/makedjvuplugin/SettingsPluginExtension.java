package com.epchain.makedjvuplugin;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.io.File;

public class SettingsPluginExtension
{
  private final Property<File> workDir;
  private final Property<File> outDir;

  public SettingsPluginExtension( Project project )
  {
    workDir = project.getObjects().property( File.class );
    outDir = project.getObjects().property( File.class );
    // Assign default values
    setWorkDir( new File(project.getProjectDir(), "tmp") );
    setOutDir( new File(project.getProjectDir(), "out") );
  }

  public File getWorkDir()
  {
    return workDir.get();
  }

  public Provider<File> getWorkDirProvider()
  {
    return workDir;
  }

  public void setWorkDir( File workDir )
  {
    this.workDir.set( workDir );
  }

  public File getOutDir()
  {
    return outDir.get();
  }

  public Provider<File> getOutDirProvider()
  {
    return outDir;
  }

  public void setOutDir( File outDir )
  {
    this.outDir.set( outDir );
  }
}
