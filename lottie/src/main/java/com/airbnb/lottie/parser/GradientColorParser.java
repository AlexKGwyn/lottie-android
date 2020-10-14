package com.airbnb.lottie.parser;

import android.graphics.Color;

import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.utils.GammaEvaluator;
import com.airbnb.lottie.utils.MiscUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;

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
   * There are {@link #colorPoints} colors sequentially as:
   * [
   * ...,
   * position,
   * red,
   * green,
   * blue,
   * ...
   * ]
   * <p>
   * The remainder of the array is the opacity stops sequentially as:
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

    return flattenToGradient(colorPositions, colors, opacityPositions, opacities);
  }

  /**
   * Flattens opacity and color positions into one final GradientColor.
   * Opacity stops can be at arbitrary intervals independent of color stops.
   * To accurately represent the gradient this interpolates additional color stops where
   * opacity stops are not aligned with a color stop.
   */
  private GradientColor flattenToGradient(
      @FloatRange(from = 0, to = 1) float[] colorPositions,
      @ColorInt int[] colors,
      @FloatRange(from = 0, to = 1) float[] opacityPositions,
      @FloatRange(from = 0, to = 1) float[] opacities) {

    ArrayList<Integer> resultColors = new ArrayList<>();
    ArrayList<Float> resultPositions = new ArrayList<>();

    int colorStops = colorPositions.length;
    int colorIndex = 0;
    float lastColorPosition = 0;
    int lastColor = colors[0] & 0x00FFFFFF;

    int opacityStops = opacityPositions.length;
    int opacityIndex = 0;
    float lastOpacityPosition = 0;
    int lastOpacity = 0;

    // TODO verify ordering assumption
    while(opacityIndex < opacityStops || colorIndex < colorStops){
      float currentColorPosition;
      int currentColor;
      float currentOpacityPosition;
      int currentOpacity;

      if (colorIndex < colorStops) {
        currentColorPosition = colorPositions[colorIndex];
        currentColor = colors[colorIndex] & 0x00FFFFFF;
      } else {
        currentColorPosition = 1;
        currentColor = lastColor;
      }

      if (opacityIndex < opacityStops) {
        currentOpacityPosition = opacityPositions[opacityIndex];
        currentOpacity = (int) (255 * opacities[opacityIndex]);
      } else {
        currentOpacityPosition = 1;
        currentOpacity = lastOpacity;
      }

      int insertColor;
      float insertPosition;

      if (currentColorPosition < currentOpacityPosition) {
        float progress = (currentColorPosition - lastOpacityPosition) / (currentOpacityPosition - lastOpacityPosition);
        insertColor = MiscUtils.lerp(lastOpacity, currentOpacity, progress) << 24 | currentColor;
        insertPosition = currentColorPosition;
      } else if (currentOpacityPosition < currentColorPosition) {
        float progress = (currentOpacityPosition - lastColorPosition) / (currentColorPosition - lastColorPosition);
        insertColor = (currentOpacity << 24) | GammaEvaluator.evaluate(progress, lastColor, currentColor);
        insertPosition = currentOpacityPosition;
      } else {
        insertColor = (currentOpacity << 24) | currentColor;
        insertPosition = currentColorPosition;
      }

      if (currentColorPosition <= currentOpacityPosition) {
        lastColor = currentColor;
        lastColorPosition = currentColorPosition;
        colorIndex++;
      }
      if (currentOpacityPosition <= currentColorPosition) {
        lastOpacity = currentOpacity;
        lastOpacityPosition = currentOpacityPosition;
        opacityIndex++;
      }
      resultColors.add(insertColor);
      resultPositions.add(insertPosition);
    }

    float[] finalPositions = new float[resultPositions.size()];
    int[] finalColors = new int[resultColors.size()];
    for (int i = 0; i < resultPositions.size(); i++) {
      finalPositions[i] = resultPositions.get(i);
      finalColors[i] = resultColors.get(i);
    }

    return new GradientColor(finalPositions, finalColors);
  }
}