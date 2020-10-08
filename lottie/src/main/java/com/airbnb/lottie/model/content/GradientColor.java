package com.airbnb.lottie.model.content;

import com.airbnb.lottie.utils.GammaEvaluator;
import com.airbnb.lottie.utils.MiscUtils;

public class GradientColor {
  private final int[] gradientColors;
  private final float[] gradientPositions;

  public GradientColor(float[] gradientPositions, int[] gradientColors) {
    this.gradientPositions = gradientPositions;
    this.gradientColors = gradientColors;
  }

  /**
   * @return The merged opacity and colors of this gradient.
   */
  public int[] getColors() {
    return gradientColors;
  }

  /**
   * @return The merged opacity and color positions of this gradient.
   */
  public float[] getPositions() {
    return gradientPositions;
  }

  /**
   * @return The size of the merged opacity and color stops for this gradient. This will always equal opacity stops + color stops
   */
  public int getSize() {
    return gradientPositions.length;
  }

  public void lerp(GradientColor gc1, GradientColor gc2, float progress) {
    if (gc1.gradientPositions.length != gc2.gradientPositions.length) {
      throw new IllegalArgumentException("Cannot interpolate between gradients. Size varies (" +
          gc1.gradientPositions.length + " vs " + gc2.gradientPositions.length + ")");
    }
    for (int i = 0; i < gc1.gradientPositions.length; i++) {
      gradientPositions[i] = MiscUtils.lerp(gc1.gradientPositions[i], gc2.gradientPositions[i], progress);
      gradientColors[i] = GammaEvaluator.evaluate(progress, gc1.gradientColors[i], gc2.gradientColors[i]);
    }
  }
}
