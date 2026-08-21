# lunar_widget.php
#!/usr/bin/env php
<?php

define('DEFAULT_LAT', 0.0);
define('DEFAULT_LON', 0.0);
define('CONFIG_FILE', 'lunar_widget_config.json');

function julianDay($dt) {
    $year = (int)$dt->format('Y');
    $month = (int)$dt->format('m');
    $day = (float)$dt->format('d') + (float)$dt->format('H')/24 + (float)$dt->format('i')/1440 + (float)$dt->format('s')/86400;
    if ($month <= 2) { $year--; $month += 12; }
    $A = (int)($year / 100);
    $B = 2 - $A + (int)($A / 4);
    return (int)(365.25 * ($year + 4716)) + (int)(30.6001 * ($month + 1)) + $day + $B - 1524.5;
}

function moonPosition($jd) {
    $T = ($jd - 2451545.0) / 36525.0;
    $L_prime = 218.3165 + 481267.8813 * $T;
    $D = 297.8502 + 445267.1114 * $T;
    $M = 357.5291 + 35999.0503 * $T;
    $M_prime = 134.9634 + 477198.8676 * $T;
    $F = 93.2720 + 483202.0175 * $T;

    $L_prime = fmod($L_prime, 360) * M_PI / 180;
    $D = fmod($D, 360) * M_PI / 180;
    $M = fmod($M, 360) * M_PI / 180;
    $M_prime = fmod($M_prime, 360) * M_PI / 180;
    $F = fmod($F, 360) * M_PI / 180;

    $lon = $L_prime + (6.289 * sin($M_prime) + 1.274 * sin(2*$D - $M_prime) + 0.658 * sin(2*$D) + 0.214 * sin(2*$M_prime) - 0.186 * sin($M) - 0.114 * sin(2*$F)) * M_PI / 180;
    $lat = (5.128 * sin($F) + 0.280 * sin($M_prime + $F) + 0.278 * sin($M_prime - $F) + 0.173 * sin(2*$D - $F)) * M_PI / 180;
    return [$lon, $lat];
}

function sunPosition($jd) {
    $T = ($jd - 2451545.0) / 36525.0;
    $M = fmod(357.5291 + 35999.0503 * $T, 360) * M_PI / 180;
    $C = 1.9146 * sin($M) + 0.0200 * sin(2*$M) + 0.0003 * sin(3*$M);
    $lon = fmod(280.4665 + 36000.7698 * $T + $C, 360) * M_PI / 180;
    return $lon;
}

function moonPhase($dt) {
    $jd = julianDay($dt);
    list($lon_moon) = moonPosition($jd);
    $lon_sun = sunPosition($jd);

    $elong = $lon_moon - $lon_sun;
    $elong = atan2(sin($elong), cos($elong));
    $phase_angle = atan2(sin($elong), cos($elong));
    $illumination = (1 + cos($phase_angle)) / 2;

    $age = ($jd - 2451550.1) / 29.53058867;
    $age = fmod($age, 29.53058867);
    if ($age < 0) $age += 29.53058867;

    if ($age < 1.0) $phase = "New Moon";
    elseif ($age < 7.38) $phase = "Waxing Crescent";
    elseif ($age < 8.38) $phase = "First Quarter";
    elseif ($age < 14.77) $phase = "Waxing Gibbous";
    elseif ($age < 15.77) $phase = "Full Moon";
    elseif ($age < 22.15) $phase = "Waning Gibbous";
    elseif ($age < 23.15) $phase = "Last Quarter";
    else $phase = "Waning Crescent";

    $signs = ["Aries","Taurus","Gemini","Cancer","Leo","Virgo","Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"];
    $lon_deg = fmod($lon_moon * 180 / M_PI, 360);
    if ($lon_deg < 0) $lon_deg += 360;
    $idx = (int)($lon_deg / 30);
    $zodiac = $signs[$idx];

    return ['phase' => $phase, 'illumination' => $illumination * 100, 'age' => $age, 'zodiac' => $zodiac];
}

function asciiMoon($illumination) {
    if ($illumination < 1) return "🌑 New Moon";
    if ($illumination < 20) return "🌒 Waxing Crescent";
    if ($illumination < 40) return "🌓 First Quarter";
    if ($illumination < 60) return "🌔 Waxing Gibbous";
    if ($illumination < 80) return "🌕 Full Moon";
    if ($illumination < 90) return "🌖 Waning Gibbous";
    if ($illumination < 98) return "🌗 Last Quarter";
    return "🌘 Waning Crescent";
}

function render($dt, $lat, $lon, $phaseOnly, $visualOnly) {
    $data = moonPhase($dt);

    if ($phaseOnly) { echo $data['phase'] . "\n"; return; }
    if ($visualOnly) { echo asciiMoon($data['illumination']) . "\n"; return; }

    echo "\n🌙 Lunar Calendar Widget\n";
    echo "Date: " . $dt->format('Y-m-d H:i') . "\n";
    echo "Location: " . round($lat, 2) . "°, " . round($lon, 2) . "°\n";
    echo "\n🌓 Phase: " . $data['phase'] . " (" . number_format($data['illumination'], 1) . "% illuminated)\n";
    echo "📅 Moon age: " . number_format($data['age'], 1) . " days\n";
    echo "♑ Zodiac: " . $data['zodiac'] . "\n";
    echo "\nVisual:\n" . asciiMoon($data['illumination']) . "\n";
}

$opts = getopt("", ["date:", "lat:", "lon:", "phase-only", "visual-only", "save-location:"]);

// Load config
$config = ['location' => ['lat' => DEFAULT_LAT, 'lon' => DEFAULT_LON]];
if (file_exists(CONFIG_FILE)) {
    $config = json_decode(file_get_contents(CONFIG_FILE), true) ?? $config;
}

if (isset($opts['save-location'])) {
    $config['location'] = [
        'name' => $opts['save-location'],
        'lat' => $opts['lat'] ?? DEFAULT_LAT,
        'lon' => $opts['lon'] ?? DEFAULT_LON
    ];
    file_put_contents(CONFIG_FILE, json_encode($config, JSON_PRETTY_PRINT));
    echo "✅ Location '" . $opts['save-location'] . "' saved.\n";
}

$lat = $opts['lat'] ?? $config['location']['lat'] ?? DEFAULT_LAT;
$lon = $opts['lon'] ?? $config['location']['lon'] ?? DEFAULT_LON;

$dt = new DateTime();
if (isset($opts['date'])) {
    $dt = new DateTime($opts['date']);
}

render($dt, $lat, $lon, isset($opts['phase-only']), isset($opts['visual-only']));
?>
