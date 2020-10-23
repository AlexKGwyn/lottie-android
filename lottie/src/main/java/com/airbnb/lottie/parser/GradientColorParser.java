package com.airbnb.lottie.parser;

import android.graphics.Color;

import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.parser.moshi.JsonReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GradientColorParser implements com.airbnb.lottie.parser.ValueParser<GradientColor> {
  /**
   * The number of colors if it exists in the json or -1 if it doesn't (legacy bodymovin)
   */
  private int colorPoints;

  public GradientColorParser(int colorPoints) {
    this.colorPoints = colorPoints;
  }

  /**
   * Both the color stops and opacity stops are in the same array.
   * There are {@link #colorPoints} colors listed sequentially by position ascending:
   * [
   * ...,
   * position,
   * red,
   * green,
   * blue,
   * ...
   * ]
   * <p>
   * The remainder of the array is the opacity stops listed sequentially by position ascending:
   * Opacity stops are optional
   * [
   * ...,
   * position,
   * opacity,
   * ...
   * ]
   */
  @Override
  public GradientColor parse(JsonReader reader, float scale)
      throws IOException {
    List<Float> array = new ArrayList<>();
    // The array was started by Keyframe because it thought that this may be an array of keyframes
    // but peek returned a number so it considered it a static array of numbers.
    boolean isArray = reader.peek() == JsonReader.Token.BEGIN_ARRAY;
    if (isArray) {
      reader.beginArray();
    }
    while (reader.hasNext()) {
      array.add((float) reader.nextDouble());
    }
    if (isArray) {
      reader.endArray();
    }
    if (colorPoints == -1) {
      colorPoints = array.size() / 4;
    }

    float[] colorPositions = new float[colorPoints];
    int[] colors = new int[colorPoints];

    int r = 0;
    int g = 0;
    for (int i = 0; i < colorPoints * 4; i++) {
      int colorIndex = i / 4;
      double value = array.get(i);
      switch (i % 4) {
        case 0:
          // position
          colorPositions[colorIndex] = (float) value;
          break;
        case 1:
          r = (int) (value * 255);
          break;
        case 2:
          g = (int) (value * 255);
          break;
        case 3:
          int b = (int) (value * 255);
          colors[colorIndex] = Color.argb(255, r, g, b);
          break;
      }
    }

    // Check if the array continues into opacity data
    int opacityStartIndex = colorPoints * 4;
    if (array.size() <= opacityStartIndex) {
      return new GradientColor(colorPositions, colors, null, null);
    }

    int opacityStops = (array.size() - opacityStartIndex) / 2;
    float[] opacityPositions = new float[opacityStops];
    float[] opacities = new float[opacityStops];

    for (int i = opacityStartIndex, j = 0; i < array.size(); i++) {
      if (i % 2 == 0) {
        opacityPositions[j] = array.get(i);
      } else {
        opacities[j] = array.get(i);
        j++;
      }
    }

    return new GradientColor(colorPositions, colors, opacityPositions, opacities);
  }

}