import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class WeatherApp extends Application {
    private static final String API_KEY = "790b041120df79dbab828a3ba171ffaf";
    private VBox historyBox = new VBox();
    private ComboBox<String> tempUnitBox;
    private ComboBox<String> windUnitBox;
    private String currentUnit = "metric";
    private ArrayList<String> searchHistory = new ArrayList<>();
    private VBox forecastBox = new VBox();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Weather Information App");

        Label locationLabel = new Label("Enter City:");
        locationLabel.setTextFill(Color.WHITE);

        TextField locationInput = new TextField();
        locationInput.setPrefWidth(250);

        Button searchButton = new Button("Get Weather");
        searchButton.setStyle("-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-weight: bold;");

        tempUnitBox = new ComboBox<>();
        tempUnitBox.getItems().addAll("Celsius", "Fahrenheit");
        tempUnitBox.setValue("Celsius");

        windUnitBox = new ComboBox<>();
        windUnitBox.getItems().addAll("m/s", "km/h");
        windUnitBox.setValue("m/s");

        VBox weatherBox = new VBox();
        weatherBox.setSpacing(10);
        forecastBox.setSpacing(5);

        searchButton.setOnAction(e -> {
            String city = locationInput.getText();
            if (!city.isEmpty()) {
                String unit = tempUnitBox.getValue().equals("Celsius") ? "metric" : "imperial";
                currentUnit = unit;
                try {
                    JSONObject weatherData = fetchWeatherData(city, unit);
                    JSONObject forecastData = fetchForecastData(city, unit);
                    if (weatherData != null) {
                        displayWeather(weatherData, weatherBox);
                        displayForecast(forecastData);
                        addToHistory(city);
                    }
                } catch (Exception ex) {
                    showAlert("Error", "Could not retrieve weather data. Check your input or try again later.");
                }
            } else {
                showAlert("Input Error", "Please enter a valid city name.");
            }
        });

        VBox inputSection = new VBox(10, locationLabel, locationInput, tempUnitBox, windUnitBox, searchButton);
        inputSection.setAlignment(Pos.CENTER_LEFT);

        Label historyLabel = new Label("Search History:");
        historyLabel.setFont(Font.font("Segoe UI", 13));
        historyLabel.setTextFill(Color.WHITE);
        historyBox.setSpacing(5);

        Label forecastLabel = new Label("3-Hour Forecast:");
        forecastLabel.setFont(Font.font("Segoe UI", 13));
        forecastLabel.setTextFill(Color.WHITE);

        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(inputSection, weatherBox, forecastLabel, forecastBox, historyLabel, historyBox);

        root.setStyle("-fx-background-color: #2c3e50;");

        Scene scene = new Scene(root, 520, 680);
        stage.setScene(scene);
        stage.show();
    }

    private JSONObject fetchWeatherData(String city, String unit) throws Exception {
        String urlString = String.format("http://api.openweathermap.org/data/2.5/weather?q=%s&units=%s&appid=%s", city, unit, API_KEY);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to fetch data");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return new JSONObject(response.toString());
    }

    private JSONObject fetchForecastData(String city, String unit) throws Exception {
        String urlString = String.format("http://api.openweathermap.org/data/2.5/forecast?q=%s&units=%s&cnt=3&appid=%s", city, unit, API_KEY);
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() != 200) {
            throw new IOException("Failed to fetch forecast");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return new JSONObject(response.toString());
    }

    private void displayWeather(JSONObject data, VBox container) {
        container.getChildren().clear();
        String city = data.getString("name");
        JSONObject main = data.getJSONObject("main");
        JSONObject wind = data.getJSONObject("wind");
        JSONObject weather = data.getJSONArray("weather").getJSONObject(0);

        double temp = main.getDouble("temp");
        int humidity = main.getInt("humidity");
        double windSpeed = wind.getDouble("speed");
        String description = weather.getString("description");
        String iconCode = weather.getString("icon");

        Image icon = new Image("http://openweathermap.org/img/wn/" + iconCode + "@2x.png");
        ImageView iconView = new ImageView(icon);
        StackPane iconWrapper = new StackPane(iconView);
        iconWrapper.setStyle("-fx-background-color: white; -fx-padding: 10px; -fx-background-radius: 10px;");
        iconWrapper.setMaxSize(100, 100);

        Text weatherText = new Text(String.format("City: %s\nTemperature: %.1f %s\nHumidity: %d%%\nWind Speed: %.1f %s\nCondition: %s",
                city, temp, tempUnitBox.getValue(), humidity, windSpeed, windUnitBox.getValue(), description));
        weatherText.setFont(Font.font("Segoe UI", 14));
        weatherText.setFill(Color.WHITE);

        container.getChildren().addAll(iconWrapper, weatherText);
    }

    private void displayForecast(JSONObject forecastData) {
        forecastBox.getChildren().clear();
        JSONArray list = forecastData.getJSONArray("list");
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            String time = entry.getString("dt_txt").split(" ")[1].substring(0, 5);
            double temp = entry.getJSONObject("main").getDouble("temp");
            String condition = entry.getJSONArray("weather").getJSONObject(0).getString("description");
            Label label = new Label(String.format("%s → %.1f %s, %s", time, temp, tempUnitBox.getValue(), condition));
            label.setTextFill(Color.LIGHTGRAY);
            forecastBox.getChildren().add(label);
        }
    }

    private void addToHistory(String city) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String entry = String.format("%s - %s", city, LocalDateTime.now().format(formatter));
        searchHistory.add(0, entry);
        updateHistoryDisplay();
    }

    private void updateHistoryDisplay() {
        historyBox.getChildren().clear();
        for (String item : searchHistory) {
            Label label = new Label(item);
            label.setTextFill(Color.LIGHTGRAY);
            historyBox.getChildren().add(label);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}