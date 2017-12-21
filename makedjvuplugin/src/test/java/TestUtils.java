import com.epchain.makedjvuplugin.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestUtils
{
  @Test
  void Capitalize()
  {
    assertEquals( "Camel", Utils.capitalize("camel") );
  }

  @Test
  void FilenameWithoutExtension()
  {
    assertEquals( "file", Utils.getFilenameWithoutExtension("file.ext") );
    assertEquals( "file.life", Utils.getFilenameWithoutExtension("file.life.ext") );
  }

  @Test
  void FileExtension()
  {
    assertEquals( "ext", Utils.getFileExtension("file.ext") );
    assertEquals( "ext", Utils.getFileExtension("file.life.ext") );
  }

  @Test
  void AlphaBlending()
  {
    // Semi-transparent color on white background
    blendingTest(
      new Color( 0,   105, 0,   141 ),
      new Color( 255, 255, 255, 0 ),
      new Color( 114, 172, 114, 0 )
    );

    // Semi-transparent color on black background
    blendingTest(
      new Color( 0, 105, 0, 141 ),
      new Color( 0, 0,   0, 0 ),
      new Color( 0, 58,  0, 0 )
    );

    // Opaque color on white background
    blendingTest(
      new Color( 0,   104, 0,   255 ),
      new Color( 255, 255, 255, 0 ),
      new Color( 0,   104, 0,   0 )
    );

    // Opaque color on black background
    blendingTest(
      new Color( 0, 104, 0, 255 ),
      new Color( 0, 0,   0, 0 ),
      new Color( 0, 104, 0, 0 )
    );
  }

  private static class Color
  {
    byte r, g, b, a;

    Color() {}

    Color( int r, int g, int b, int a )
    {
      this.r = (byte) r;
      this.g = (byte) g;
      this.b = (byte) b;
      this.a = (byte) a;
    }
  }

  private static void blendingTest( Color src, Color bg, Color target )
  {
    Color result = new Color();

    result.r = Utils.alphaBlend( src.r, src.a, bg.r );
    result.g = Utils.alphaBlend( src.g, src.a, bg.g );
    result.b = Utils.alphaBlend( src.b, src.a, bg.b );

    assertEquals( target.r, result.r );
    assertEquals( target.g, result.g );
    assertEquals( target.b, result.b );
  }
}
