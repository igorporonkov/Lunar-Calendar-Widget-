// lunar_widget.go
package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"math"
	"os"
	"time"
)

const (
	DEFAULT_LAT = 0.0
	DEFAULT_LON = 0.0
	CONFIG_FILE = "lunar_widget_config.json"
)

type Config struct {
	Location struct {
		Name string  `json:"name"`
		Lat  float64 `json:"lat"`
		Lon  float64 `json:"lon"`
	} `json:"location"`
}

type LunarData struct {
	Phase        string
	Illumination float64
	Age          float64
	Zodiac       string
}

func julianDay(t time.Time) float64 {
	year := t.Year()
	month := int(t.Month())
	day := float64(t.Day()) + float64(t.Hour())/24.0 + float64(t.Minute())/1440.0 + float64(t.Second())/86400.0
	if month <= 2 {
		year--
		month += 12
	}
	A := year / 100
	B := 2 - A + A/4
	return float64(int(365.25*float64(year+4716))) + float64(int(30.6001*float64(month+1))) + day + float64(B) - 1524.5
}

func moonPosition(jd float64) (float64, float64) {
	T := (jd - 2451545.0) / 36525.0
	L_prime := 218.3165 + 481267.8813*T
	D := 297.8502 + 445267.1114*T
	M := 357.5291 + 35999.0503*T
	M_prime := 134.9634 + 477198.8676*T
	F := 93.2720 + 483202.0175*T

	L_prime = math.Mod(L_prime, 360) * math.Pi / 180
	D = math.Mod(D, 360) * math.Pi / 180
	M = math.Mod(M, 360) * math.Pi / 180
	M_prime = math.Mod(M_prime, 360) * math.Pi / 180
	F = math.Mod(F, 360) * math.Pi / 180

	lon := L_prime + (6.289*math.Sin(M_prime)+1.274*math.Sin(2*D-M_prime)+0.658*math.Sin(2*D)+0.214*math.Sin(2*M_prime)-0.186*math.Sin(M)-0.114*math.Sin(2*F))*math.Pi/180
	lat := (5.128*math.Sin(F)+0.280*math.Sin(M_prime+F)+0.278*math.Sin(M_prime-F)+0.173*math.Sin(2*D-F))*math.Pi/180
	return lon, lat
}

func sunPosition(jd float64) float64 {
	T := (jd - 2451545.0) / 36525.0
	M := math.Mod(357.5291+35999.0503*T, 360) * math.Pi / 180
	C := 1.9146*math.Sin(M) + 0.0200*math.Sin(2*M) + 0.0003*math.Sin(3*M)
	lon := math.Mod(280.4665+36000.7698*T+C, 360) * math.Pi / 180
	return lon
}

func moonPhase(t time.Time) LunarData {
	jd := julianDay(t)
	lonMoon, _ := moonPosition(jd)
	lonSun := sunPosition(jd)

	elong := lonMoon - lonSun
	elong = math.Atan2(math.Sin(elong), math.Cos(elong))
	phaseAngle := math.Atan2(math.Sin(elong), math.Cos(elong))
	illumination := (1 + math.Cos(phaseAngle)) / 2

	age := (jd - 2451550.1) / 29.53058867
	age = math.Mod(age, 29.53058867)
	if age < 0 {
		age += 29.53058867
	}

	var phase string
	if age < 1.0 {
		phase = "New Moon"
	} else if age < 7.38 {
		phase = "Waxing Crescent"
	} else if age < 8.38 {
		phase = "First Quarter"
	} else if age < 14.77 {
		phase = "Waxing Gibbous"
	} else if age < 15.77 {
		phase = "Full Moon"
	} else if age < 22.15 {
		phase = "Waning Gibbous"
	} else if age < 23.15 {
		phase = "Last Quarter"
	} else {
		phase = "Waning Crescent"
	}

	signs := []string{"Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"}
	lonDeg := math.Mod(lonMoon*180/math.Pi, 360)
	if lonDeg < 0 {
		lonDeg += 360
	}
	idx := int(lonDeg / 30)
	zodiac := signs[idx]

	return LunarData{Phase: phase, Illumination: illumination * 100, Age: age, Zodiac: zodiac}
}

func asciiMoon(illumination float64) string {
	if illumination < 1 {
		return "🌑 New Moon"
	}
	if illumination < 20 {
		return "🌒 Waxing Crescent"
	}
	if illumination < 40 {
		return "🌓 First Quarter"
	}
	if illumination < 60 {
		return "🌔 Waxing Gibbous"
	}
	if illumination < 80 {
		return "🌕 Full Moon"
	}
	if illumination < 90 {
		return "🌖 Waning Gibbous"
	}
	if illumination < 98 {
		return "🌗 Last Quarter"
	}
	return "🌘 Waning Crescent"
}

func render(t time.Time, lat, lon float64, phaseOnly, visualOnly bool) {
	data := moonPhase(t)

	if phaseOnly {
		fmt.Println(data.Phase)
		return
	}
	if visualOnly {
		fmt.Println(asciiMoon(data.Illumination))
		return
	}

	fmt.Printf("\n🌙 Lunar Calendar Widget\n")
	fmt.Printf("Date: %s\n", t.Format("2006-01-02 15:04"))
	fmt.Printf("Location: %.2f°, %.2f°\n", lat, lon)
	fmt.Printf("\n🌓 Phase: %s (%.1f%% illuminated)\n", data.Phase, data.Illumination)
	fmt.Printf("📅 Moon age: %.1f days\n", data.Age)
	fmt.Printf("♑ Zodiac: %s\n", data.Zodiac)
	fmt.Printf("\nVisual:\n%s\n", asciiMoon(data.Illumination))
}

func main() {
	var (
		dateStr      = flag.String("date", "", "YYYY-MM-DD")
		lat          = flag.Float64("lat", DEFAULT_LAT, "Latitude (positive North)")
		lon          = flag.Float64("lon", DEFAULT_LON, "Longitude (positive East)")
		phaseOnly    = flag.Bool("phase-only", false, "Output only phase name")
		visualOnly   = flag.Bool("visual-only", false, "Show only moon visual")
		saveLocation = flag.String("save-location", "", "Save location with name")
	)
	flag.Parse()

	// Load config
	var config Config
	if data, err := os.ReadFile(CONFIG_FILE); err == nil {
		json.Unmarshal(data, &config)
	}

	if *saveLocation != "" {
		config.Location.Name = *saveLocation
		config.Location.Lat = *lat
		config.Location.Lon = *lon
		data, _ := json.MarshalIndent(config, "", "  ")
		os.WriteFile(CONFIG_FILE, data, 0644)
		fmt.Printf("✅ Location '%s' saved.\n", *saveLocation)
	}

	// Use config values if flags not provided
	if *lat == DEFAULT_LAT && *lon == DEFAULT_LON {
		*lat = config.Location.Lat
		*lon = config.Location.Lon
	}

	t := time.Now()
	if *dateStr != "" {
		t, _ = time.Parse("2006-01-02", *dateStr)
	}

	render(t, *lat, *lon, *phaseOnly, *visualOnly)
}
