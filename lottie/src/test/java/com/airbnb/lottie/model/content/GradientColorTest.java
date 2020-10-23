package com.airbnb.lottie.model.content;

import com.airbnb.lottie.BaseTest;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class GradientColorTest extends BaseTest {

  @Test
  public void testSimpleGradientMerge(){
    float[] colorPositions = {0, .5f, 1};
    int[] colors = {0x000000, 0xFFFFFF, 0x000000};
    float[] opacityPositions = {0, .5f, 1};
    float[] opacities = {1, 0, 1};
    GradientColor gradientColor = new GradientColor(colorPositions, colors, opacityPositions, opacities);

    assertArrayEquals(gradientColor.getColors(), new int[]{0xFF000000, 0x00FFFFFF, 0xFF000000});
    assertArrayEquals(gradientColor.getPositions(), new float[]{0, .5f, 1}, .01f);
  }

  @Test
  public void testOffsetGradientMerge(){
    float[] colorPositions = {0, 1};
    int[] colors = {0x000000, 0xFFFFFF};
    float[] opacityPositions = {0, .5f, 1};
    float[] opacities = {1, .5f, 0};
    GradientColor gradientColor = new GradientColor(colorPositions, colors, opacityPositions, opacities);

    assertArrayEquals(gradientColor.getColors(), new int[]{0xFF000000, 0x7F7F7F7F, 0x00FFFFFF});
    assertArrayEquals(gradientColor.getPositions(), new float[]{0, .5f, 1}, .01f);
  }
}
