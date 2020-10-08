package com.airbnb.lottie.parser;

import android.graphics.Color;

import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.utils.GammaEvaluator;
import com.airbnb.lottie.utils.MiscUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.core.math.MathUtils;

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
      return new GradientColor(colorPositions, colors);
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

    // Combine the opacity and color information into a single array
    int size = colorPositions.length + opacityPositions.length;
    float[] gradientPositions = new float[size];
    int[] gradientColors = new int[size];
    mergeGradientStops(colorPositions, colors, opacityPositions, opacities, gradientPositions, gradientColors);
    return new GradientColor(gradientPositions, gradientColors);
  }

  private void mergeGradientStops(float[] colorPositions, int[] colorStops, float[] opacityPositions, float[] opacityStops, float[] gradientPositions, int[] gradientColors) {
    int colorIndex = 0;
    int opacityIndex = 0;
    float lastPosition = 0;
    int lastColor = (int) (255 * opacityStops[0]) << 24 | (colorStops[0] & 0x00FFFFFF);
    while (opacityIndex < opacityPositions.length || colorIndex < colorPositions.length) {
      float colorPosition;
      int color;
      float opacityPosition;
      int opacity;

      if (colorIndex < colorPositions.length) {
        colorPosition = colorPositions[colorIndex];
        color = colorStops[colorIndex] & 0x00FFFFFF;
      } else {
        colorPosition = 1;
        color = lastColor & 0x00FFFFFF;
      }

      if (opacityIndex < opacityPositions.length) {
        opacityPosition = opacityPositions[opacityIndex];
        opacity = (int) (255 * opacityStops[opacityIndex]);
      } else {
        opacityPosition = 1;
        opacity = Color.alpha(lastColor);
      }

      // Positions are guaranteed to be ordered.
      int insertColor = interpolateNextColor(lastPosition, lastColor, opacityPosition, opacity, colorPosition, color);
      float insertPosition = Math.min(colorPosition, opacityPosition);

      gradientColors[colorIndex+opacityIndex] = insertColor;
      gradientPositions[colorIndex+opacityIndex] = insertPosition;

      if (colorIndex < colorPositions.length && colorPosition <= opacityPosition) {
        colorIndex++;
      } else {
        opacityIndex++;
      }

      lastColor = insertColor;
      lastPosition = insertPosition;
    }
  }

  /**
   * Interpolate the next color at either the colorPosition or the opacityPosition, whichever is lower.
   * @return the next color
   */
  private int interpolateNextColor(float lastPosition, int lastColor, float opacityPosition, int opacity, float colorPosition, int color) {
    float opacityDelta = opacityPosition - lastPosition;
    float colorDelta = colorPosition - lastPosition;
    float colorProgress = colorDelta > 0 ? MathUtils.clamp(opacityDelta / colorDelta, 0, 1) : 0;
    float opacityProgress = opacityDelta > 0 ? MathUtils.clamp(colorDelta / opacityDelta, 0, 1) : 0;

    int opacityComponent = MiscUtils.lerp(Color.alpha(lastColor), opacity, opacityProgress) << 24;
    int colorComponent = GammaEvaluator.evaluate(colorProgress, lastColor & 0x00FFFFFF, color);
    return opacityComponent | colorComponent;
  }


}