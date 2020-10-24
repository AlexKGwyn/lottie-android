package com.airbnb.lottie.model.content;

import android.graphics.Color;

import com.airbnb.lottie.utils.GammaEvaluator;
import com.airbnb.lottie.utils.MiscUtils;

import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;


public class GradientColor {
  private final float[] colorPositions;
  private final int[] colorStops;
  @Nullable private final float[] opacityPositions;
  @Nullable private final float[] opacityStops;

  // Arrays holding the merged gradient colors. Can be new arrays or references to their respective color arrays.
  private int[] gradientColors;
  private float[] gradientPositions;

  public GradientColor(float[] colorPositions, int[] colorStops, @Nullable float[] opacityPositions, @Nullable float[] opacityStops) {
    this.colorPositions = colorPositions;
    this.colorStops = colorStops;
    this.opacityPositions = opacityPositions;
    this.opacityStops = opacityStops;

    if (opacityPositions == null || opacityStops == null) {
      gradientColors = colorStops;
      gradientPositions = colorPositions;
    } else {
      initArrays();
      mergeGradientStops();
    }
  }

  private void initArrays() {
    int colorIndex = 0;
    int opacityIndex = 0;
    int positionCount = 0;
    while (opacityIndex < opacityPositions.length && colorIndex < colorPositions.length) {
      float opacityPosition = opacityPositions[opacityIndex];
      float colorPosition = colorPositions[colorIndex];
      if (opacityPosition == colorPosition) {
        positionCount++;
        opacityIndex++;
        colorIndex++;
      } else if (opacityPosition < colorPosition) {
        opacityIndex++;
        positionCount++;
      } else {
        colorIndex++;
        positionCount++;
      }
    }

    positionCount += (opacityPositions.length - opacityIndex) + (colorPositions.length - colorIndex);
    if (positionCount == colorPositions.length) {
      gradientPositions = colorPositions;
      gradientColors = colorStops;
    } else if (gradientPositions == null || gradientPositions.length != positionCount) {
      gradientPositions = new float[positionCount];
      gradientColors = new int[positionCount];
    }
  }

  private void mergeGradientStops() {
    int colorIndex = 0;
    int opacityIndex = 0;
    int resultIndex = 0;
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

      int insertColor = interpolateNextColor(lastPosition, lastColor, opacityPosition, opacity, colorPosition, color);
      float insertPosition = Math.min(colorPosition, opacityPosition);

      if (colorPosition <= opacityPosition) {
        colorIndex++;
      }
      if (opacityPosition <= colorPosition) {
        opacityIndex++;
      }

      gradientColors[resultIndex] = insertColor;
      gradientPositions[resultIndex] = insertPosition;

      resultIndex++;
      lastColor = insertColor;
      lastPosition = insertPosition;
    }
  }

  // Interpolate the next color at either the colorPosition or the opacityPosition, whichever is lower
  private int interpolateNextColor(float lastPosition, int lastColor, float opacityPosition, int opacity, float colorPosition, int color) {
    float opacityDelta = opacityPosition - lastPosition;
    float colorDelta = colorPosition - lastPosition;
    float colorProgress = colorDelta > 0 ? MathUtils.clamp(opacityDelta / colorDelta, 0, 1) : 0;
    float opacityProgress = opacityDelta > 0 ? MathUtils.clamp(colorDelta / opacityDelta, 0, 1) : 0;

    int opacityComponent = MiscUtils.lerp(Color.alpha(lastColor), opacity, opacityProgress) << 24;
    int colorComponent = GammaEvaluator.evaluate(colorProgress, lastColor & 0x00FFFFFF, color);
    return opacityComponent | colorComponent;
  }

  /**
   * @return The merged opacity and colors of this gradient.
   */
  public int[] getGradientColors() {
    return gradientColors;
  }

  /**
   * @return The merged opacity and color positions of this gradient.
   */
  public float[] getGradientPositions() {
    return gradientPositions;
  }

  /**
   * @return The positions of the color stops of this gradient. Will not include opacity
   */
  public float[] getColorStopPositions() {
    return colorPositions;
  }

  /**
   * @return The color stops of this gradient. Will not include opacity
   */
  public int[] getColorStops() {
    return colorStops;
  }

  /**
   * @return The positions of the opacity stops of this gradient. Will not include color information
   */
  public float[] getOpacityStopPositions() {
    return opacityPositions;
  }

  /**
   * @return The opacity stops of this gradient. Will not include color information
   */
  public float[] getOpacityStops() {
    return opacityStops;
  }

  public int getColorSize() {
    return colorStops.length;
  }

  public boolean hasOpacityStops() {
    return opacityStops != null && opacityPositions != null;
  }

  public int getOpacitySize() {
    return opacityStops.length;
  }

  public void lerp(GradientColor gc1, GradientColor gc2, float progress) {
    if (gc1.colorStops.length != gc2.colorStops.length) {
      throw new IllegalArgumentException("Cannot interpolate between gradients. Color stops vary (" +
          gc1.colorStops.length + " vs " + gc2.colorStops.length + ")");
    }
    for (int i = 0; i < gc1.colorStops.length; i++) {
      colorPositions[i] = MiscUtils.lerp(gc1.colorPositions[i], gc2.colorPositions[i], progress);
      colorStops[i] = GammaEvaluator.evaluate(progress, gc1.colorStops[i], gc2.colorStops[i]);
    }

    if (gc1.hasOpacityStops() && gc2.hasOpacityStops()) {
      if (gc1.opacityStops.length != gc2.opacityStops.length) {
        throw new IllegalArgumentException("Cannot interpolate between gradients. Opacity stops vary (" +
            gc1.opacityStops.length + " vs " + gc2.opacityStops.length + ")");
      }

      for (int i = 0; i < gc1.opacityStops.length; i++) {
        opacityPositions[i] = MiscUtils.lerp(gc1.opacityPositions[i], gc2.opacityPositions[i], progress);
        opacityStops[i] = MiscUtils.lerp(gc1.opacityStops[i], gc2.opacityStops[i], progress);
      }
    }

    if (hasOpacityStops()) {
      initArrays();
      mergeGradientStops();
    } else {
      gradientColors = colorStops;
      gradientPositions = colorPositions;
    }
  }
}
