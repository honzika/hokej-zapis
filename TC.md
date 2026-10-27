# Test casy – Hokejový zápis

Ověřeno na Android emulátoru (Medium Phone API 36.1) 27. 10. 2026. iOS zatím neověřeno (chybí Xcode).

| # | Scénář | Kroky | Očekávaný výsledek | Android |
|---|--------|-------|--------------------|---------|
| 1 | Nastavení zápasu | Přepsat název týmu, zvolit barvu, délku třetiny −/+ | Název max 24 znaků, vybraná barva má bílý rámeček, délka 1–20 min | OK |
| 2 | Start zápasu | „Začít zápas“ | 1. třetina z 3, čas = délka třetiny, „Klepnutím spustíš čas“ | OK |
| 3 | Časomíra | Klepnout na čas, počkat, klepnout znovu | Běží (zelená), zastaví se, po pauze pokračuje od stejného času | OK |
| 4 | Korekce času | −10s / −1s / +1s / +10s | Čas se posune, nepřekročí délku třetiny ani nejde pod 0 | OK |
| 5 | Gól se střelcem | „+ Gól“, napsat střelce, Enter/Uložit | Klávesnice vyskočí sama; skóre +1; v událostech třetina, čas a střelec | OK |
| 6 | Čas gólu | „+ Gól“ a chvíli vyplňovat střelce | Gól má čas stisku tlačítka (viditelný v titulku dialogu), ne čas uložení | OK |
| 7 | Gól bez střelce | „+ Gól“ → „Bez střelce“ | Skóre +1, v událostech jen název týmu | OK |
| 8 | Smazání gólu | Ikona koše → „Smazat“ | Gól zmizí, skóre −1 | OK |
| 9 | Konec třetiny | Nechat doběhnout čas na 0:00 | „Konec třetiny“, tlačítko „Začít N. třetinu“; +čas třetinu znovu otevře | OK |
| 10 | Konec zápasu | Po 3. třetině „Konec zápasu“ | Souhrn: výsledek, góly po třetinách, střelci | OK |
| 11 | Předčasné ukončení | Vlajka v liště nebo systémové Zpět | Dialog „Ukončit zápas?“ se skóre; „Pokračovat“ nic nezmění | OK |
| 12 | Rotace | Otočit telefon během zápasu | Stav i běžící čas zůstanou; na šířku týmy po stranách, čas uprostřed | OK |
| 13 | Nový zápas | V souhrnu „Nový zápas“ | Nastavení předvyplněné posledními hodnotami | OK |
| 14 | Displej | Nechat zápas otevřený | Displej během zápasu nezhasne | neověřeno (emulátor) |
| 15 | TV scoreboard (demo) | Otevřít `receiver/index.html?demo` | Týmy v barvách, skóre, běžící čas, poslední gól | OK (prohlížeč) |
| 16 | Unit testy | `./gradlew :composeApp:testDebugUnitTest` | 9/9 zelené | OK |
