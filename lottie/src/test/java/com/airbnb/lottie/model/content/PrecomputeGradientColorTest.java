package com.airbnb.lottie.model.content;

import com.airbnb.lottie.BaseTest;
import com.airbnb.lottie.parser.ComputedGradientColorParser;
import com.airbnb.lottie.parser.PreComputedGradientColorParser;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.google.common.base.Charsets;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okio.Buffer;

public class PrecomputeGradientColorTest extends BaseTest {

  public PrecomputeGradientColorTest() {

  }

  @Test
  public void testParse() throws IOException {
    float[] points =
        new float[]{0f, 0.969f, 0.514f, 0.745f,
            0.116f, 0.984f, 0.537f, 0.373f,
            1f, 1f, 0.559f, 0f,
            0.607f, 0f, 0.776f, 0.5f,
            0.945f, 1f};
    PreComputedGradientColor pre = makePrecomputed(points, 4);
    ComputedGradientColor computed = makeComputed(points, 4);
    isSame(computed, pre);
  }

  @Test
  public void testInterpolation() throws IOException {
    int colorSize = 3;
    // float[] points1 = new float[]{
    //     0f, 0.365f, 0.387f, 1f,
    //     0.5f, 0.182f, 0.202f, 0.638f,
    //     1f, 0f, 0.018f, 0.277f,
    //     0f, 1f,
    //     0.5f, 1f,
    //     1f, 1f
    // };
    // float[] points2 = new float[]{
    //     0.5f, 0.365f, 0.387f, 1f,
    //     0.75f, 0.182f, 0.202f, 0.638f,
    //     1f, 0f, 0.018f, 0.277f,
    //     0f, 1f,
    //     0.5f, 1f,
    //     1f, 1f
    // };

    float[] points1 = new float[]{
        0f, 0f, 0f, 1f,
        0.7f, 0f, 0f, .5f,
        1f, 0f, 0f, 0f,
        0f, 1f,
        0.3f, 0f,
        1f, 1f
    };
    float[] points2 = new float[]{
        0f, 0f, 0f, 1f,
        0.3f, 0f, 0f, .5f,
        1f, 0f, 0f, 0f,
        0f, 1f,
        0.7f, 0f,
        1f, 1f
    };

    PreComputedGradientColor pre1 = makePrecomputed(points1, colorSize);
    PreComputedGradientColor pre2 = makePrecomputed(points2, colorSize);
    ComputedGradientColor computed1 = makeComputed(points1, colorSize);
    ComputedGradientColor computed2 = makeComputed(points2, colorSize);
    //isSame(computed1, pre1);
    //isSame(computed2, pre2);

    System.out.println("Pre 1" + toString(pre1));
    System.out.println("Pre 2" + toString(pre2));
    System.out.println("Comp 1" + toString(computed1));
    System.out.println("Comp 2" + toString(computed2));

    for (float i = 0; i <= 1; i = (float) (i + .5)) {
      PreComputedGradientColor pre = new PreComputedGradientColor(new float[pre1.getSize()], new int[pre1.getSize()]);
      ComputedGradientColor comp = new ComputedGradientColor(new float[computed1.getColorSize()], new int[computed1.getColorSize()],
          new float[computed1.getOpacitySize()], new float[computed1.getOpacitySize()]);
      pre.lerp(pre1, pre2, i);
      comp.lerp(computed1, computed2, i);
      System.out.println("**********************\ni=" + i);
      System.out.println("Pre " + toString(pre));
      System.out.println("Comp " + toString(comp));
      //isSame(comp, pre);
    }
  }


  private ComputedGradientColor makeComputed(float[] points, int size) throws IOException {
    Buffer buffer = new Buffer();
    buffer.writeString(toJsonString(points), Charsets.UTF_8);
    JsonReader reader = JsonReader.of(buffer);
    ComputedGradientColorParser computedParser = new ComputedGradientColorParser(size);
    return (ComputedGradientColor) computedParser.parse(reader, 1f);
  }

  private PreComputedGradientColor makePrecomputed(float[] points, int size) throws IOException {
    Buffer buffer = new Buffer();
    buffer.writeString(toJsonString(points), Charsets.UTF_8);
    JsonReader reader = JsonReader.of(buffer);
    PreComputedGradientColorParser computedParser = new PreComputedGradientColorParser(size);
    return (PreComputedGradientColor) computedParser.parse(reader, 1f);
  }

  private void isSame(ComputedGradientColor computed, PreComputedGradientColor precompute) {
    ArrayList<Integer> reducedColorsList = new ArrayList<>();
    ArrayList<Float> reducedPositionsList = new ArrayList<>();
    float prev = 0;
    for (int i = 0; i < precompute.getPositions().length; i++) {
      float position = precompute.getPositions()[i];
      if (position != prev || i == 0) {
        reducedColorsList.add(precompute.getColors()[i]);
        reducedPositionsList.add(position);
      }
      prev = position;
    }

    int[] precomputedColors = toIntArray(reducedColorsList);
    float[] precomputedPositions = toFloatArray(reducedPositionsList);
    System.out.println("Pre  " + toString(precompute) + "\nComp " + toString(computed));
    System.out.println("Pre  " + toString(precompute) + "\nComp " + toString(computed));
    // assertArrayEquals("Pre  " + toString(precompute) + "\nComp " + toString(computed),
    //     precomputedColors, computed.getColors());
    // assertArrayEquals("Pre  " + toString(precompute) + "\nComp " + toString(computed), precomputedPositions,
    //     computed.getPositions(), .01f);
  }

  int[] toIntArray(List<Integer> list) {
    int[] ret = new int[list.size()];
    int i = 0;
    for (Integer e : list) {
      ret[i++] = e;
    }
    return ret;
  }

  float[] toFloatArray(List<Float> list) {
    float[] ret = new float[list.size()];
    int i = 0;
    for (Float e : list) {
      ret[i++] = e;
    }
    return ret;
  }

  String toJsonString(float[] arr) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    boolean first = true;
    for (float val : arr) {
      if (!first) {
        builder.append(",");
      }
      builder.append(val);
      first = false;
    }
    builder.append("]");
    return builder.toString();
  }

  String toString(GradientColor gradientColor) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    float lastPosition = -1f;
    for (int i = 0; i < gradientColor.getSize(); i++) {
      float position = gradientColor.getPositions()[i];
      if(position > lastPosition) {
        lastPosition = position;
        if (i != 0) {
          builder.append(",");
        }
        String color = String.format("#%08X", gradientColor.getColors()[i]);
        builder.append(color + "/" + position);
      }
    }
    builder.append("]");
    return builder.toString();
  }
}
