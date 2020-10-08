package com.airbnb.lottie.model.content;

import com.airbnb.lottie.utils.GammaEvaluator;
import com.airbnb.lottie.utils.MiscUtils;

public class PreComputedGradientColor implements GradientColor {
  private final int[] gradientColors;
  private final float[] gradientPositions;

  public PreComputedGradientColor(float[] gradientPositions, int[] gradientColors) {
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
    if (gc1 instanceof PreComputedGradientColor && gc2 instanceof PreComputedGradientColor) {
      PreComputedGradientColor pgc1 = (PreComputedGradientColor) gc1;
      PreComputedGradientColor pgc2 = (PreComputedGradientColor) gc2;
      if (pgc1.gradientPositions.length != pgc2.gradientPositions.length) {
        throw new IllegalArgumentException("Cannot interpolate between gradients. Size varies (" +
            pgc1.gradientPositions.length + " vs " + pgc2.gradientPositions.length + ")");
      }
      for (int i = 0; i < pgc1.gradientPositions.length; i++) {
        gradientPositions[i] = MiscUtils.lerp(pgc1.gradientPositions[i], pgc2.gradientPositions[i], progress);
        gradientColors[i] = GammaEvaluator.evaluate(progress, pgc1.gradientColors[i], pgc2.gradientColors[i]);
      }
    }
    else {
      throw new IllegalArgumentException("No");
    }
  }
}
