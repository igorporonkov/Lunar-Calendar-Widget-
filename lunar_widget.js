// lunar_widget.js
#!/usr/bin/env node
const fs = require('fs');
const { program } = require('commander');

const DEFAULT_LAT = 0.0;
const DEFAULT_LON = 0.0;
const CONFIG_FILE = 'lunar_widget_config.json';

function julianDay(date) {
    let year = date.getFullYear();
    let month = date.getMonth() + 1;
    let day = date.getDate() + date.getHours()/24 + date.getMinutes()/1440 + date.getSeconds()/86400;
    if (month <= 2) { year--; month += 12; }
    let A = Math.floor(year / 100);
    let B = 2 - A + Math.floor(A / 4);
    return Math.floor(365.25 * (year + 4716)) + Math.floor(30.6001 * (month + 1)) + day + B - 1524.5;
}

function moonPosition(jd) {
    let T = (jd - 2451545.0) / 36525.0;
    let L_prime = 218.3165 + 481267.8813 * T;
    let D = 297.8502 + 445267.1114 * T;
    let M = 357.5291 + 35999.0503 * T;
    let M_prime = 134.9634 + 477198.8676 * T;
    let F = 93.2720 + 483202.0175 * T;

    L_prime = ((L_prime % 360) + 360) % 360 * Math.PI / 180;
    D = ((D % 360) + 360) % 360 * Math.PI / 180;
    M = ((M % 360) + 360) % 360 * Math.PI / 180;
    M_prime = ((M_prime % 360) + 360) % 360 * Math.PI / 180;
    F = ((F % 360) + 360) % 360 * Math.PI / 180;

    let lon = L_prime + (6.289 * Math.sin(M_prime) + 1.274 * Math.sin(2*D - M_prime) + 0.658 * Math.sin(2*D) + 0.214 * Math.sin(2*M_prime) - 0.186 * Math.sin(M) - 0.114 * Math.sin(2*F)) * Math.PI / 180;
    let lat = (5.128 * Math.sin(F) + 0.280 * Math.sin(M_prime + F) + 0.278 * Math.sin(M_prime - F) + 0.173 * Math.sin(2*D - F)) * Math.PI / 180;
    return { lon, lat };
}

function sunPosition(jd) {
    let T = (jd - 2451545.0) / 36525.0;
    let M = ((357.5291 + 35999.0503 * T) % 360) * Math.PI / 180;
    let C = 1.9146 * Math.sin(M) + 0.0200 * Math.sin(2*M) + 0.0003 * Math.sin(3*M);
    let lon = ((280.4665 + 36000.7698 * T + C) % 360) * Math.PI / 180;
    return lon;
}

function moonPhase(date) {
    let jd = julianDay(date);
    let { lon: lonMoon } = moonPosition(jd);
    let lonSun = sunPosition(jd);

    let elong = lonMoon - lonSun;
    elong = Math.atan2(Math.sin(elong), Math.cos(elong));
    let phaseAngle = Math.atan2(Math.sin(elong), Math.cos(elong));
    let illumination = (1 + Math.cos(phaseAngle)) / 2;

    let age = (jd - 2451550.1) / 29.53058867;
    age = ((age % 29.53058867) + 29.53058867) % 29.53058867;

    let phase;
    if (age < 1.0) phase = "New Moon";
    else if (age < 7.38) phase = "Waxing Crescent";
    else if (age < 8.38) phase = "First Quarter";
    else if (age < 14.77) phase = "Waxing Gibbous";
    else if (age < 15.77) phase = "Full Moon";
    else if (age < 22.15) phase = "Waning Gibbous";
    else if (age < 23.15) phase = "Last Quarter";
    else phase = "Waning Crescent";

    const signs = ["Aries","Taurus","Gemini","Cancer","Leo","Virgo","Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"];
    let lonDeg = ((lonMoon * 180 / Math.PI) % 360 + 360) % 360;
    let idx = Math.floor(lonDeg / 30);
    let zodiac = signs[idx];

    return { phase, illumination: illumination * 100, age, zodiac };
}

function asciiMoon(illumination) {
    if (illumination < 1) return "🌑 New Moon";
    if (illumination < 20) return "🌒 Waxing Crescent";
    if (illumination < 40) return "🌓 First Quarter";
    if (illumination < 60) return "🌔 Waxing Gibbous";
    if (illumination < 80) return "🌕 Full Moon";
    if (illumination < 90) return "🌖 Waning Gibbous";
    if (illumination < 98) return "🌗 Last Quarter";
    return "🌘 Waning Crescent";
}

function render(date, lat, lon, phaseOnly, visualOnly) {
    let data = moonPhase(date);

    if (phaseOnly) { console.log(data.phase); return; }
    if (visualOnly) { console.log(asciiMoon(data.illumination)); return; }

    console.log(`\n🌙 Lunar Calendar Widget`);
    console.log(`Date: ${date.toISOString().slice(0,16).replace('T',' ')}`);
    console.log(`Location: ${lat.toFixed(2)}°, ${lon.toFixed(2)}°`);
    console.log(`\n🌓 Phase: ${data.phase} (${data.illumination.toFixed(1)}% illuminated)`);
    console.log(`📅 Moon age: ${data.age.toFixed(1)} days`);
    console.log(`♑ Zodiac: ${data.zodiac}`);
    console.log(`\nVisual:\n${asciiMoon(data.illumination)}`);
}

program
    .option('--date <date>', 'YYYY-MM-DD')
    .option('--lat <lat>', 'Latitude (positive North)', parseFloat, DEFAULT_LAT)
    .option('--lon <lon>', 'Longitude (positive East)', parseFloat, DEFAULT_LON)
    .option('--phase-only', 'Output only phase name')
    .option('--visual-only', 'Show only moon visual')
    .option('--save-location <name>', 'Save location with name')
    .parse(process.argv);

const opts = program.opts();

// Load config
let config = { location: { lat: DEFAULT_LAT, lon: DEFAULT_LON } };
if (fs.existsSync(CONFIG_FILE)) {
    try {
        config = JSON.parse(fs.readFileSync(CONFIG_FILE));
    } catch (e) {}
}

if (opts.saveLocation) {
    config.location = { name: opts.saveLocation, lat: opts.lat, lon: opts.lon };
    fs.writeFileSync(CONFIG_FILE, JSON.stringify(config, null, 2));
    console.log(`✅ Location '${opts.saveLocation}' saved.`);
}

// Use config values if flags not provided
let lat = opts.lat || config.location.lat || DEFAULT_LAT;
let lon = opts.lon || config.location.lon || DEFAULT_LON;

let dt = new Date();
if (opts.date) {
    dt = new Date(opts.date + 'T00:00:00');
}

render(dt, lat, lon, opts.phaseOnly, opts.visualOnly);
