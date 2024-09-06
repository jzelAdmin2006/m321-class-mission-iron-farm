package com.jzel;

import static java.lang.Math.abs;
import static java.util.Objects.requireNonNull;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Main {

  public static final Gson GSON = new Gson();
  public static final MediaType JSON = MediaType.parse("application/json");
  public static final int CARGO_HOLD_HEIGHT = 10;
  public static final int CARGO_HOLD_WIDTH = 12;
  private static final String BASE_URL = System.getenv("BASE_URL");
  public static final Request BUY_IRON_REQUEST = new Builder()
      .url(BASE_URL + ":2011/buy")
      .post(RequestBody.create("{\"station\": \"Vesta Station\", \"what\": \"IRON\", \"amount\": 12}", JSON))
      .build();
  public static final Request GET_POS_REQUEST = new Builder().url(BASE_URL + ":2011/pos").get().build();
  private static final OkHttpClient CLIENT = new OkHttpClient();
  private static final double VESTA_X = 10000;
  private static final double VESTA_Y = 10000;
  private static final double ARAK_X = 2490;
  private static final double ARAK_Y = 4368;


  public static void main(final String[] args) throws IOException, InterruptedException {
    checkEmpty120CargoHold();
    if (get(HoldCredits.class, ":2012/hold").hold().credits() < 600) {
      throw new IllegalStateException("Not enough credits to buy 120 iron");
    }
    farmIron();
  }

  private static void checkEmpty120CargoHold() throws IOException {
    final Structure structure = getStructure();
    final List<List<String>> holds = structure.getHold();
    if (holds.size() != CARGO_HOLD_HEIGHT || holds.stream().anyMatch(row -> row.size() != CARGO_HOLD_WIDTH)) {
      throw new IllegalStateException("Unexpected cargo hold size");
    }
    if (holds.stream().flatMap(List::stream).anyMatch(Objects::nonNull)) {
      throw new IllegalStateException("Cargo has to be empty in order to start filling it entirely with iron");
    }
  }

  private static void farmIron() throws IOException, InterruptedException {
    setTarget(VESTA_X, VESTA_Y);
    waitUntilVestaCoords();
    for (int i = 0; i < CARGO_HOLD_HEIGHT; i++) {
      buyIron();
      moveRowToEnd();
    }
    setTarget(ARAK_X, ARAK_Y);
  }

  private static void moveRowToEnd() throws IOException, InterruptedException {
    List<List<String>> holds = getStructure().getHold();
    int firstNonEmptyRowIndex = IntStream.range(1, holds.size())
        .filter(index -> holds.get(index).stream().anyMatch(Objects::nonNull))
        .findFirst().orElse(CARGO_HOLD_HEIGHT);
    for (int currentRow = 0; currentRow < firstNonEmptyRowIndex - 1; currentRow++) {
      swapCargoHoldsRow(currentRow, currentRow + 1);
      TimeUnit.MILLISECONDS.sleep(700);
    }
  }

  private static void buyIron() throws IOException {
    try (Response response = CLIENT.newCall(BUY_IRON_REQUEST).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }
    }
  }

  private static void setTarget(final double x, final double y) throws IOException {
    try (final Response response = CLIENT.newCall(new Request.Builder()
        .url(BASE_URL + ":2009/set_target")
        .post(RequestBody.create("{\"target\": {\"x\": %s, \"y\": %s}}".formatted(x, y), JSON))
        .build()).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }
    }
  }

  private static void waitUntilVestaCoords()
      throws IOException, InterruptedException {
    Position position;
    do {
      Thread.sleep(1000);
      position = getPos();
    } while (abs(position.pos().x() - VESTA_X) > 30 || abs(position.pos().y() - VESTA_Y) > 30 || Stream.of(
            position.velocity().x(),
            position.velocity().y(), position.velocity().angle())
        .anyMatch(v -> abs(v) > 4));
  }

  private static Position getPos() throws IOException {
    try (final Response response = new OkHttpClient().newCall(GET_POS_REQUEST).execute()) {
      return GSON.fromJson(requireNonNull(response.body()).string(), Position.class);
    }
  }

  private static void swapCargoHoldsRow(int yFrom, int yTo) throws IOException, InterruptedException {
    for (int x = 0; x <= 11; x++) {
      swapCargoHolds(x, yFrom, x, yTo);
      TimeUnit.MILLISECONDS.sleep(700);
    }
  }

  private static void swapCargoHolds(int xFrom, int yFrom, int xTo, int yTo) throws IOException {
    final Request request = new Request.Builder()
        .url(BASE_URL + ":2012/swap_adjacent")
        .post(RequestBody.create(
            "{\"a\": {\"x\": %s, \"y\": %s}, \"b\": {\"x\": %s, \"y\": %s}}".formatted(xFrom, yFrom, xTo, yTo), JSON))
        .build();

    try (final Response response = CLIENT.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }
    }
  }

  private static Structure getStructure() throws IOException {
    return get(Structure.class, ":2012/structure");
  }

  private static <T> T get(final Class<T> clazz, final String url) throws IOException {
    Request request = new Request.Builder()
        .url(BASE_URL + url)
        .build();

    try (final Response response = CLIENT.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }
      return GSON.fromJson(requireNonNull(response.body()).string(), clazz);
    }
  }
}