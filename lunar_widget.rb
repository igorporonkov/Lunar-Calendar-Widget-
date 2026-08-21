# lunar_widget.rb
#!/usr/bin/env ruby
require 'json'
require 'date'
require 'optparse'

DEFAULT_LAT = 0.0
DEFAULT_LON = 0.0
CONFIG_FILE = 'lunar_widget_config.json'

def julian_day(dt)
  year = dt.year
  month = dt.month
  day = dt.day + dt.hour/24.0 + dt.min/1440.0 + dt.sec/86400.0
  if month <= 2
    year -= 1
    month += 12
  end
  a = (year / 100).to_i
  b = 2 - a + (a / 4).to_i
  (365.25 * (year + 4716)).to_i + (30.6001 * (month + 1)).to_i + day + b - 1524.5
end

def moon_position(jd)
  t = (jd - 2451545.0) / 36525.0
  l_prime = 218.3165 + 481267.8813 * t
  d = 297.8502 + 445267.1114 * t
  m = 357.5291 + 35999.0503 * t
  m_prime = 134.9634 + 477198.8676 * t
  f = 93.2720 + 483202.0175 * t

  l_prime = (l_prime % 360) * Math::PI / 180
  d = (d % 360) * Math::PI / 180
  m = (m % 360) * Math::PI / 180
  m_prime = (m_prime % 360) * Math::PI / 180
  f = (f % 360) * Math::PI / 180

  lon = l_prime + (6.289 * Math.sin(m_prime) + 1.274 * Math.sin(2*d - m_prime) + 0.658 * Math.sin(2*d) + 0.214 * Math.sin(2*m_prime) - 0.186 * Math.sin(m) - 0.114 * Math.sin(2*f)) * Math::PI / 180
  lat = (5.128 * Math.sin(f) + 0.280 * Math.sin(m_prime + f) + 0.278 * Math.sin(m_prime - f) + 0.173 * Math.sin(2*d - f)) * Math::PI / 180
  [lon, lat]
end

def sun_position(jd)
  t = (jd - 2451545.0) / 36525.0
  m = (357.5291 + 35999.0503 * t) % 360 * Math::PI / 180
  c = 1.9146 * Math.sin(m) + 0.0200 * Math.sin(2*m) + 0.0003 * Math.sin(3*m)
  lon = (280.4665 + 36000.7698 * t + c) % 360 * Math::PI / 180
  lon
end

def moon_phase(dt)
  jd = julian_day(dt)
  lon_moon, _ = moon_position(jd)
  lon_sun = sun_position(jd)

  elong = lon_moon - lon_sun
  elong = Math.atan2(Math.sin(elong), Math.cos(elong))
  phase_angle = Math.atan2(Math.sin(elong), Math.cos(elong))
  illumination = (1 + Math.cos(phase_angle)) / 2

  age = (jd - 2451550.1) / 29.53058867
  age = age % 29.53058867

  phase = case age
          when 0...1.0 then "New Moon"
          when 1.0...7.38 then "Waxing Crescent"
          when 7.38...8.38 then "First Quarter"
          when 8.38...14.77 then "Waxing Gibbous"
          when 14.77...15.77 then "Full Moon"
          when 15.77...22.15 then "Waning Gibbous"
          when 22.15...23.15 then "Last Quarter"
          else "Waning Crescent"
          end

  signs = ["Aries","Taurus","Gemini","Cancer","Leo","Virgo","Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"]
  lon_deg = (lon_moon * 180 / Math::PI) % 360
  idx = (lon_deg / 30).to_i
  zodiac = signs[idx]

  { phase: phase, illumination: illumination * 100, age: age, zodiac: zodiac }
end

def ascii_moon(illumination)
  if illumination < 1
    "🌑 New Moon"
  elsif illumination < 20
    "🌒 Waxing Crescent"
  elsif illumination < 40
    "🌓 First Quarter"
  elsif illumination < 60
    "🌔 Waxing Gibbous"
  elsif illumination < 80
    "🌕 Full Moon"
  elsif illumination < 90
    "🌖 Waning Gibbous"
  elsif illumination < 98
    "🌗 Last Quarter"
  else
    "🌘 Waning Crescent"
  end
end

def render(dt, lat, lon, phase_only, visual_only)
  data = moon_phase(dt)

  if phase_only
    puts data[:phase]
    return
  end
  if visual_only
    puts ascii_moon(data[:illumination])
    return
  end

  puts "\n🌙 Lunar Calendar Widget"
  puts "Date: #{dt.strftime('%Y-%m-%d %H:%M')}"
  puts "Location: #{lat.round(2)}°, #{lon.round(2)}°"
  puts "\n🌓 Phase: #{data[:phase]} (#{data[:illumination].round(1)}% illuminated)"
  puts "📅 Moon age: #{data[:age].round(1)} days"
  puts "♑ Zodiac: #{data[:zodiac]}"
  puts "\nVisual:"
  puts ascii_moon(data[:illumination])
end

options = {}
OptionParser.new do |opts|
  opts.banner = "Usage: lunar_widget.rb [options]"
  opts.on("--date DATE", "YYYY-MM-DD") { |v| options[:date] = v }
  opts.on("--lat LAT", Float, "Latitude (positive North)") { |v| options[:lat] = v }
  opts.on("--lon LON", Float, "Longitude (positive East)") { |v| options[:lon] = v }
  opts.on("--phase-only", "Output only phase name") { options[:phase_only] = true }
  opts.on("--visual-only", "Show only moon visual") { options[:visual_only] = true }
  opts.on("--save-location NAME", "Save location with name") { |v| options[:save_location] = v }
end.parse!

# Load config
config = { "location" => { "lat" => DEFAULT_LAT, "lon" => DEFAULT_LON } }
if File.exist?(CONFIG_FILE)
  config = JSON.parse(File.read(CONFIG_FILE)) rescue config
end

if options[:save_location]
  config["location"] = {
    "name" => options[:save_location],
    "lat" => options[:lat] || DEFAULT_LAT,
    "lon" => options[:lon] || DEFAULT_LON
  }
  File.write(CONFIG_FILE, JSON.pretty_generate(config))
  puts "✅ Location '#{options[:save_location]}' saved."
end

lat = options[:lat] || config["location"]["lat"] || DEFAULT_LAT
lon = options[:lon] || config["location"]["lon"] || DEFAULT_LON

dt = DateTime.now
if options[:date]
  dt = DateTime.parse(options[:date])
end

render(dt, lat, lon, options[:phase_only], options[:visual_only])
