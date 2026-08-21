🌙 Lunar Calendar Widget — Multi‑Language Moon Phase Display
8 languages, one beautiful lunar widget – display current moon phase, illumination, age, and zodiac sign with an ASCII visual – right from your terminal.

✨ Features
🌓 Current moon phase – New Moon, Waxing Crescent, First Quarter, Waxing Gibbous, Full Moon, Waning Gibbous, Last Quarter, Waning Crescent

📊 Illumination percentage – exact fraction of the moon's visible disk

📅 Moon age – days since last New Moon (0–29.53)

♑ Zodiac sign – moon's current zodiac constellation

🎨 ASCII moon visualization – a beautiful text‑based rendering

🌍 Optional location – specify latitude/longitude for rise/set times

⏱️ Live update – show the current state or a specific date

💾 Configuration file – save your preferred location

🚀 Quick Start
bash
# Show today's moon phase (default)
<command>

# Show with custom location (for rise/set times)
<command> --lat 48.8584 --lon 2.2945

# Show for a specific date
<command> --date 2026-12-25

# Show just the visual moon
<command> --visual-only

# Show all details (default)
<command> --all

# Show only the phase name
<command> --phase-only
📸 Example Output
text
🌙 Lunar Calendar Widget
Date: 2026-08-21 14:30 UTC
Location: 48.86°N, 2.29°E

🌓 Phase: Waxing Gibbous (85.2% illuminated)
📅 Moon age: 10.8 days
♑ Zodiac: Sagittarius

Visual:
    ████████
  ████████████
 ██████████████
 ████████░░░░░░
 ████████░░░░░░
  ████████████
    ████████

Moonrise: 16:45 | Moonset: 02:30
📁 Repository Structure
text
.
├── README.md
├── python/
│   └── lunar_widget.py
├── go/
│   └── lunar_widget.go
├── javascript/
│   └── lunar_widget.js
├── ruby/
│   └── lunar_widget.rb
├── php/
│   └── lunar_widget.php
├── java/
│   └── LunarWidget.java
├── csharp/
│   └── LunarWidget.cs
└── cpp/
    └── lunar_widget.cpp
