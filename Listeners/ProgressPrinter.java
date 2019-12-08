import java.math.BigInteger;
import java.math.BigDecimal;

import java.io.PrintStream;

public class ProgressPrinter {
  static final int maxTurns = 4;
  static final String[] turns = {"\\", "|", "/", "-"};
  long numCvPerPrint = 1;
  long numCvSinceLastPrint = 0;
  int currentTurnNumber = 0;

  public void printProgress(final double percentProgress, final long observed, final long expected) {
    numCvSinceLastPrint++;
    numCvSinceLastPrint %= numCvPerPrint;

    if (numCvSinceLastPrint != 0) {
      return;
    }

    final long progress = (long) round(percentProgress);

    System.out.print("\r");
    printPercentCovered(progress);
    printProgressBar(progress, observed, expected);
    printTurnstile();
  }

  private void printPercentCovered(final long percentProgress) {
    System.out.print(" ");
    printPercentage(percentProgress);
    System.out.print("  ");

  }

  private void printProgressBar(final long percentProgress, final long observed, final long expected) {
    System.out.print("[");
    printTicks(percentProgress);
    printSpaces(100 - percentProgress);
    System.out.print("]    ");

    System.out.print(observed + " / " + expected + " ");
  }

  private void printTurnstile() {
    System.out.print("  ");
    System.out.print(turns[currentTurnNumber++]);
    currentTurnNumber = currentTurnNumber % maxTurns;
    System.out.print("  ");
  }

  private void printPercentage(final long percentage) {
    // Need to place hold space for the 100s place
    if (percentage < 100.0) {
      System.out.print(" ");
    }

    // Need to place hold space for the 10s place
    if (percentage < 10.0) {
      System.out.print(" ");
    }

    System.out.print(percentage + "%");
  }

  private void printSpaces(long numLeft) {
    for (int i = 0; i < numLeft; ++i) {
      System.out.print(" ");
    }
  }

  private void printTicks(long numLeft) {
    for (int i = 0; i < numLeft; ++i) {
      System.out.print("#");
    }
  }

  // Round to first decimal place. Couldn't find a Math.* func for this
  private float round(final float in) {
    return (float) (Math.round(in * 10) / 10.0);
  }

  private double round(final double in) {
    return (double) (Math.round(in * 10) / 10.0);
  }

  private long roundedFraction(final BigInteger part, final BigInteger whole) {
    BigDecimal oneHundred = new BigDecimal(100);
    BigDecimal percentProgress;

    percentProgress = oneHundred.multiply((new BigDecimal(part)).divide(new BigDecimal(whole), 3, BigDecimal.ROUND_HALF_EVEN));

    return percentProgress.longValue();
  }
}
