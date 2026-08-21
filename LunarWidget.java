// LunarWidget.java
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import com.google.gson.*;

class Config {
    public Location location = new Location();
}

class Location {
    public String name = "Default";
    public double lat = 0.0;
    public double lon = 0.0;
}

class LunarData {
    String phase;
    double illumination;
    double age;
    String zodiac;
}

public class LunarWidget {
    private static final double DEFAULT_LAT = 0.0;
    private static final double DEFAULT_LON = 0.0;
    private static final String CONFIG_FILE = "lunar_widget_config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static double julianDay(LocalDateTime dt) {
        int year = dt.getYear();
        int month = dt.getMonthValue();
        double day = dt.getDayOfMonth() + dt.getHour()/24.0 + dt.getMinute()/1440.0 + dt.getSecond()/86400.0;
        if (month <= 2) { year--; month += 12; }
        int A = year / 100;
        int B = 2 - A + A / 4;
        return (int)(365.25 * (year + 4716)) + (int)(30.6001 * (month + 1)) + day + B - 1524.5;
    }

    public static double[] moonPosition(double jd) {
        double T = (jd - 2451545.0) / 36525.0;
        double L_prime = 218.3165 + 481267.8813 * T;
        double D = 297.8502 + 445267.1114 * T;
        double M = 357.5291 + 35999.0503 * T;
        double M_prime = 134.9634 + 477198.8676 * T;
        double F = 93.2720 + 483202.0175 * T;

        L_prime = ((L_prime % 360) + 360) % 360 * Math.PI / 180;
        D = ((D % 360) + 360) % 360 * Math.PI / 180;
        M = ((M % 360) + 360) % 360 * Math.PI / 180;
        M_prime = ((M_prime % 360) + 360) % 360 * Math.PI / 180;
        F = ((F % 360) + 360) % 360 * Math.PI / 180;

        double lon = L_prime + (6.289 * Math.sin(M_prime) + 1.274 * Math.sin(2*D - M_prime) + 0.658 * Math.sin(2*D) + 0.214 * Math.sin(2*M_prime) - 0.186 * Math.sin(M) - 0.114 * Math.sin(2*F)) * Math.PI / 180;
        double lat = (5.128 * Math.sin(F) + 0.280 * Math.sin(M_prime + F) + 0.278 * Math.sin(M_prime - F) + 0.173 * Math.sin(2*D - F)) * Math.PI / 180;
        return new double[]{lon, lat};
    }

    public static double sunPosition(double jd) {
        double T = (jd - 2451545.0) / 36525.0;
        double M = ((357.5291 + 35999.0503 * T) % 360) * Math.PI / 180;
        double C = 1.9146 * Math.sin(M) + 0.0200 * Math.sin(2*M) + 0.0003 * Math.sin(3*M);
        double lon = ((280.4665 + 36000.7698 * T + C) % 360) * Math.PI / 180;
        return lon;
    }

    public static LunarData moonPhase(LocalDateTime dt) {
        double jd = julianDay(dt);
        double[] moon = moonPosition(jd);
        double lonMoon = moon[0];
        double lonSun = sunPosition(jd);

        double elong = lonMoon - lonSun;
        elong = Math.atan2(Math.sin(elong), Math.cos(elong));
        double phaseAngle = Math.atan2(Math.sin(elong), Math.cos(elong));
        double illumination = (1 + Math.cos(phaseAngle)) / 2;

        double age = (jd - 2451550.1) / 29.53058867;
        age = ((age % 29.53058867) + 29.53058867) % 29.53058867;

        String phase;
        if (age < 1.0) phase = "New Moon";
        else if (age < 7.38) phase = "Waxing Crescent";
        else if (age < 8.38) phase = "First Quarter";
        else if (age < 14.77) phase = "Waxing Gibbous";
        else if (age < 15.77) phase = "Full Moon";
        else if (age < 22.15) phase = "Waning Gibbous";
        else if (age < 23.15) phase = "Last Quarter";
        else phase = "Waning Crescent";

        String[] signs = {"Aries","Taurus","Gemini","Cancer","Leo","Virgo","Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"};
        double lonDeg = ((lonMoon * 180 / Math.PI) % 360 + 360) % 360;
        int idx = (int)(lonDeg / 30);
        String zodiac = signs[idx];

        LunarData data = new LunarData();
        data.phase = phase;
        data.illumination = illumination * 100;
        data.age = age;
        data.zodiac = zodiac;
        return data;
    }

    public static String asciiMoon(double illumination) {
        if (illumination < 1) return "🌑 New Moon";
        if (illumination < 20) return "🌒 Waxing Crescent";
        if (illumination < 40) return "🌓 First Quarter";
        if (illumination < 60) return "🌔 Waxing Gibbous";
        if (illumination < 80) return "🌕 Full Moon";
        if (illumination < 90) return "🌖 Waning Gibbous";
        if (illumination < 98) return "🌗 Last Quarter";
        return "🌘 Waning Crescent";
    }

    public static void render(LocalDateTime dt, double lat, double lon, boolean phaseOnly, boolean visualOnly) {
        LunarData data = moonPhase(dt);

        if (phaseOnly) { System.out.println(data.phase); return; }
        if (visualOnly) { System.out.println(asciiMoon(data.illumination)); return; }

        System.out.printf("\n🌙 Lunar Calendar Widget\n");
        System.out.printf("Date: %s\n", dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        System.out.printf("Location: %.2f°, %.2f°\n", lat, lon);
        System.out.printf("\n🌓 Phase: %s (%.1f%% illuminated)\n", data.phase, data.illumination);
        System.out.printf("📅 Moon age: %.1f days\n", data.age);
        System.out.printf("♑ Zodiac: %s\n", data.zodiac);
        System.out.printf("\nVisual:\n%s\n", asciiMoon(data.illumination));
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                String key = args[i].substring(2);
                if (i+1 < args.length && !args[i+1].startsWith("--")) {
                    params.put(key, args[++i]);
                } else {
                    params.put(key, "");
                }
            }
        }

        // Load config
        Config config = new Config();
        if (Files.exists(Paths.get(CONFIG_FILE))) {
            String json = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            config = gson.fromJson(json, Config.class);
        }

        if (params.containsKey("save-location")) {
            config.location.name = params.get("save-location");
            config.location.lat = Double.parseDouble(params.getOrDefault("lat", String.valueOf(DEFAULT_LAT)));
            config.location.lon = Double.parseDouble(params.getOrDefault("lon", String.valueOf(DEFAULT_LON)));
            Files.write(Paths.get(CONFIG_FILE), gson.toJson(config).getBytes());
            System.out.println("✅ Location '" + config.location.name + "' saved.");
        }

        double lat = params.containsKey("lat") ? Double.parseDouble(params.get("lat")) : config.location.lat;
        double lon = params.containsKey("lon") ? Double.parseDouble(params.get("lon")) : config.location.lon;

        LocalDateTime dt = LocalDateTime.now();
        if (params.containsKey("date")) {
            dt = LocalDate.parse(params.get("date")).atStartOfDay();
        }

        render(dt, lat, lon, params.containsKey("phase-only"), params.containsKey("visual-only"));
    }
}
