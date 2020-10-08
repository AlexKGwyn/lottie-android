package com.airbnb.lottie.model.content;

public interface GradientColor {

  /**
   * @return The merged opacity and colors of this gradient.
   */
  public int[] getColors();

  /**
   * @return The merged opacity and color positions of this gradient.
   */
  public float[] getPositions();

  public int getSize();

  public void lerp(GradientColor gc1, GradientColor gc2, float progress);

}
