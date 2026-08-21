# lunar_widget.py
import math
import json
import os
import argparse
from datetime import datetime, timedelta

CONFIG_FILE = "lunar_widget_config.json"
DEFAULT_LAT = 0.0
DEFAULT_LON = 0.0

class LunarWidget:
    def __init__(self, lat=DEFAULT_LAT, lon=DEFAULT_LON):
        self.lat = lat
        self.lon = lon
        self.config = self.load_config()

    def load_config(self):
        if os.path.exists(CONFIG_FILE):
            with open(CONFIG_FILE, "r") as f:
                return json.load(f)
        return {"location": {"name": "Default", "lat": DEFAULT_LAT, "lon": DEFAULT_LON}}

    def save_config(self, name, lat, lon):
        self.config["location"] = {"name": name, "lat": lat, "lon": lon}
        with open(CONFIG_FILE, "w") as f:
            json.dump(self.config, f, indent=2)

    def julian_day(self, dt):
        year = dt.year
        month = dt.month
        day = dt.day + dt.hour/24.0 + dt.minute/1440.0 + dt.second/86400.0
        if month <= 2:
            year -= 1
            month += 12
        A = int(year / 100)
        B = 2 - A + int(A / 4)
        return int(365.25 * (year + 4716)) + int(30.6001 * (month + 1)) + day + B - 1524.5

    def moon_position(self, jd):
        T = (jd - 2451545.0) / 36525.0
        L_prime = 218.3165 + 481267.8813 * T
        D = 297.8502 + 445267.1114 * T
        M = 357.5291 + 35999.0503 * T
        M_prime = 134.9634 + 477198.8676 * T
        F = 93.2720 + 483202.0175 * T

        L_prime = ((L_prime % 360) + 360) % 360 * math.pi / 180
        D = ((D % 360) + 360) % 360 * math.pi / 180
        M = ((M % 360) + 360) % 360 * math.pi / 180
        M_prime = ((M_prime % 360) + 360) % 360 * math.pi / 180
        F = ((F % 360) + 360) % 360 * math.pi / 180

        lon = L_prime + (6.289 * math.sin(M_prime) + 1.274 * math.sin(2*D - M_prime) +
                         0.658 * math.sin(2*D) + 0.214 * math.sin(2*M_prime) -
                         0.186 * math.sin(M) - 0.114 * math.sin(2*F)) * math.pi / 180
        lat = (5.128 * math.sin(F) + 0.280 * math.sin(M_prime + F) +
               0.278 * math.sin(M_prime - F) + 0.173 * math.sin(2*D - F)) * math.pi / 180
        return lon, lat

    def sun_position(self, jd):
        T = (jd - 2451545.0) / 36525.0
        M = ((357.5291 + 35999.0503 * T) % 360) * math.pi / 180
        C = 1.9146 * math.sin(M) + 0.0200 * math.sin(2*M) + 0.0003 * math.sin(3*M)
        lon = ((280.4665 + 36000.7698 * T + C) % 360) * math.pi / 180
        return lon

    def moon_phase(self, dt):
        jd = self.julian_day(dt)
        lon_moon, _ = self.moon_position(jd)
        lon_sun = self.sun_position(jd)

        elong = lon_moon - lon_sun
        elong = math.atan2(math.sin(elong), math.cos(elong))
        phase_angle = math.atan2(math.sin(elong), math.cos(elong))
        illumination = (1 + math.cos(phase_angle)) / 2

        age = (jd - 2451550.1) / 29.53058867
        age = ((age % 29.53058867) + 29.53058867) % 29.53058867

        if age < 1.0:
            phase = "New Moon"
        elif age < 7.38:
            phase = "Waxing Crescent"
        elif age < 8.38:
            phase = "First Quarter"
        elif age < 14.77:
            phase = "Waxing Gibbous"
        elif age < 15.77:
            phase = "Full Moon"
        elif age < 22.15:
            phase = "Waning Gibbous"
        elif age < 23.15:
            phase = "Last Quarter"
        else:
            phase = "Waning Crescent"

        signs = ["Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
                 "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"]
        lon_deg = ((lon_moon * 180 / math.pi) % 360 + 360) % 360
        idx = int(lon_deg / 30)
        zodiac = signs[idx]

        return {"phase": phase, "illumination": illumination * 100,
                "age": age, "zodiac": zodiac, "jd": jd}

    def ascii_moon(self, illumination):
        """Draw a beautiful ASCII moon."""
        if illumination < 1:
            return "🌑 New Moon"
        if illumination < 20:
            return "🌒 Waxing Crescent"
        if illumination < 40:
            return "🌓 First Quarter"
        if illumination < 60:
            return "🌔 Waxing Gibbous"
        if illumination < 80:
            return "🌕 Full Moon"
        if illumination < 90:
            return "🌖 Waning Gibbous"
        if illumination < 98:
            return "🌗 Last Quarter"
        return "🌘 Waning Crescent"

    def render(self, dt, phase_only=False, visual_only=False):
        data = self.moon_phase(dt)

        if phase_only:
            print(data["phase"])
            return
        if visual_only:
            print(self.ascii_moon(data["illumination"]))
            return

        print(f"\n🌙 Lunar Calendar Widget")
        print(f"Date: {dt.strftime('%Y-%m-%d %H:%M')}")
        print(f"Location: {self.lat:.2f}°, {self.lon:.2f}°")
        print(f"\n🌓 Phase: {data['phase']} ({data['illumination']:.1f}% illuminated)")
        print(f"📅 Moon age: {data['age']:.1f} days")
        print(f"♑ Zodiac: {data['zodiac']}")
        print(f"\nVisual:")
        print(self.ascii_moon(data["illumination"]))

def main():
    parser = argparse.ArgumentParser(description="Lunar Calendar Widget")
    parser.add_argument("--date", help="YYYY-MM-DD")
    parser.add_argument("--lat", type=float, help="Latitude (positive North)")
    parser.add_argument("--lon", type=float, help="Longitude (positive East)")
    parser.add_argument("--phase-only", action="store_true", help="Output only phase name")
    parser.add_argument("--visual-only", action="store_true", help="Show only moon visual")
    parser.add_argument("--all", action="store_true", help="Show all details")
    parser.add_argument("--save-location", help="Save location with name")
    args = parser.parse_args()

    lat = args.lat if args.lat is not None else DEFAULT_LAT
    lon = args.lon if args.lon is not None else DEFAULT_LON

    widget = LunarWidget(lat, lon)

    if args.save_location:
        widget.save_config(args.save_location, lat, lon)
        print(f"✅ Location '{args.save_location}' saved.")

    dt = datetime.now()
    if args.date:
        dt = datetime.strptime(args.date, "%Y-%m-%d")

    widget.render(dt, args.phase_only, args.visual_only)

if __name__ == "__main__":
    main()
