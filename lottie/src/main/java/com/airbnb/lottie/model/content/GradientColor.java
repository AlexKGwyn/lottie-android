package com.airbnb.lottie.model.content;

import android.graphics.Color;

import com.airbnb.lottie.utils.GammaEvaluator;
import com.airbnb.lottie.utils.MiscUtils;

import androidx.core.math.MathUtils;


public class GradientColor {
  private final float[] colorPositions;
  private final int[] colorComponents;
  private final float[] opacityPositions;
  private final float[] opacityComponents;

  private int[] gradientColors;
  private float[] gradientPositions;

  public GradientColor(float[] colorPositions, int[] colorComponents, float[] opacityPositions, float[] opacityComponents) {
    this.colorPositions = colorPositions;
    this.colorComponents = colorComponents;
    this.opacityPositions = opacityPositions;
    this.opacityComponents = opacityComponents;

    if (opacityPositions == null) {
      gradientColors = colorComponents;
      gradientPositions = colorPositions;
    } else {
      initArrays();
      mergeGradientComponents();
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
    if (gradientPositions == null || gradientPositions.length != positionCount) {
      gradientPositions = new float[positionCount];
      gradientColors = new int[positionCount];
    }
  }

  private void mergeGradientComponents() {
    int colorIndex = 0;
    int opacityIndex = 0;
    int resultIndex = 0;
    float lastPosition = 0;
    int lastColor = (int) (255 * opacityComponents[0]) << 24 | (colorComponents[0] & 0x00FFFFFF);
    while (opacityIndex < opacityPositions.length || colorIndex < colorPositions.length) {
      float colorPosition;
      int color;
      float opacityPosition;
      int opacity;

      if (colorIndex < colorPositions.length) {
        colorPosition = colorPositions[colorIndex];
        color = colorComponents[colorIndex] & 0x00FFFFFF;
      } else {
        colorPosition = 1;
        color = lastColor & 0x00FFFFFF;
      }

      if (opacityIndex < opacityPositions.length) {
        opacityPosition = opacityPositions[opacityIndex];
        opacity = (int) (255 * opacityComponents[opacityIndex]);
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
    float colorProgress = opacityDelta > 0 ? MathUtils.clamp(colorDelta / opacityDelta, 0, 1) : 0;
    float opacityProgress = colorDelta > 0 ? MathUtils.clamp(opacityDelta / colorDelta, 0, 1) : 0;

    int opacityComponent = MiscUtils.lerp(Color.alpha(lastColor), opacity, opacityProgress) << 24;
    int colorComponent = GammaEvaluator.evaluate(colorProgress, lastColor & 0x00FFFFFF, color);
    return opacityComponent | colorComponent;
  }

  public int[] getColors() {
    return gradientColors;
  }

  public float[] getPositions() {
    return gradientPositions;
  }

  public int getColorSize() {
    return colorComponents.length;
  }

  public boolean hasOpacityComponents() {
    return opacityComponents != null;
  }

  public int getOpacitySize() {
    return opacityComponents.length;
  }

  public void lerp(GradientColor gc1, GradientColor gc2, float progress) {
    if (gc1.colorComponents.length != gc2.colorComponents.length) {
      throw new IllegalArgumentException("Cannot interpolate between gradients. Color stops vary (" +
          gc1.colorComponents.length + " vs " + gc2.colorComponents.length + ")");
    }
    for (int i = 0; i < gc1.colorComponents.length; i++) {
      colorPositions[i] = MiscUtils.lerp(gc1.colorPositions[i], gc2.colorPositions[i], progress);
      colorComponents[i] = GammaEvaluator.evaluate(progress, gc1.colorComponents[i], gc2.colorComponents[i]);
    }

    if (gc1.hasOpacityComponents() && gc2.hasOpacityComponents()) {
      if (gc1.opacityComponents.length != gc2.opacityComponents.length) {
        throw new IllegalArgumentException("Cannot interpolate between gradients. Opacity stops vary (" +
            gc1.opacityComponents.length + " vs " + gc2.opacityComponents.length + ")");
      }

      for (int i = 0; i < gc1.opacityComponents.length; i++) {
        colorPositions[i] = MiscUtils.lerp(gc1.opacityPositions[i], gc2.opacityPositions[i], progress);
        opacityComponents[i] = MiscUtils.lerp(progress, gc1.opacityComponents[i], gc2.opacityComponents[i]);
      }
    }

    initArrays();
    mergeGradientComponents();
  }
}
