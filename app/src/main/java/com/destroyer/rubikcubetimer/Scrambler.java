package com.destroyer.rubikcubetimer;

/**
 * Scrambler.java
 * <p/>
 * Rubik's Cube scramble generator
 *
 * @author Elliot Penson
 */

import java.util.Random;

public class Scrambler
{

  /**
   * The faces of a Rubik's Cube represented as side/axis
   */
  private static final Face[] faces = {new Face('R', 'X'),
      new Face('L', 'X'), new Face('U', 'Y'), new Face('D', 'Y'),
      new Face('F', 'Z'), new Face('B', 'Z')};
  /**
   * Possible directions of a face turn
   */
  private static final String[] rotation = {"", "'", "", "'", "2"}; // added duplicates to decrease probability of '2' turns
  /**
   * Random number generator
   */
  private static Random random = new Random(System.currentTimeMillis());

  /**
   * Generates a random 25 move scramble
   *
   * @return
   */
  public static String generateScramble()
  {
    String scramble = "";

    Face penultimate = new Face();
    Face last = new Face();

    for (int i = 0; i < 25; i++)
    {
      Face current = randomFace(penultimate, last);
      scramble += current.getSide() + randomDirection() + " ";
      penultimate = last;
      last = current;
    }

    return scramble;
  }

  /**
   * Finds a random Face that's not the same as the last or on the same axis as the last three
   *
   * @param penultimate
   * @param last
   * @return
   */
  public static Face randomFace(Face penultimate, Face last)
  {
    Face toReturn = faces[random.nextInt(faces.length)];
    if (last.sameFace(toReturn) || sameAxis(penultimate, last, toReturn))
      return randomFace(penultimate, last);
    return toReturn;
  }

  /**
   * Compares three face's axes
   *
   * @return true if all the Faces have the same axis
   */
  public static boolean sameAxis(Face a, Face b, Face c)
  {
    return a.getAxis() == b.getAxis() && b.getAxis() == c.getAxis();
  }

  /**
   * Returns a random direction for a face rotation
   *
   * @return
   */
  public static String randomDirection()
  {
    return rotation[random.nextInt(rotation.length)];
  }
}
