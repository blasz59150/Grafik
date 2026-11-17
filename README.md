# Kalendarz Brygadowy – Android prototype 0.1

Pierwszy prototyp natywnej aplikacji Android (Kotlin + Jetpack Compose).

## Co działa w prototypie
- kalendarz miesięczny;
- widok własnej brygady, wszystkich brygad i widok z notatkami;
- obliczanie znanego cyklu 16-dniowego dla czterech brygad;
- lokalny zapis SQLite: typ nieobecności, notatka i zmiana wyjątkowa;
- edycja dnia po dotknięciu komórki;
- działanie offline.

## Dane demonstracyjne
Data bazowa brygady I: 13.11.2026. Brygady II–IV są przesunięte o 4, 8 i 12 dni.
To ustawienie demonstracyjne – w kolejnej iteracji dodamy ekran konfiguracji.

## Budowanie APK
Otwórz projekt w Android Studio i wybierz Build > Build APK(s).
Albo wypchnij projekt do GitHub – workflow `.github/workflows/build-apk.yml` zbuduje APK.
Wynik: `app/build/outputs/apk/debug/app-debug.apk`.

## Ograniczenia wersji 0.1
- brak widżetu (zaplanowany następny etap);
- brak eksportu/importu backupu;
- brak automatycznych świąt i pełnego rozliczania DW;
- brak synchronizacji między urządzeniami;
- widok wszystkich brygad jest poglądowy; wpisy edycyjne są lokalne dla urządzenia.
