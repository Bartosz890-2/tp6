# Gra w GO (Java Client-Server)

### Wymagania systemowe
Aby uruchomić projekt, w systemie muszą być zainstalowane:

- **Java JDK (wersja 11 lub nowsza)**

- **Apache Maven**

### Kompilacja projektu
Przed pierwszym uruchomieniem należy zbudować projekt, aby pobrać zależności i skompilować pliki źródłowe. W głównym katalogu projektu wykonaj komendę:
```
mvn clean compile
```

### Instrukcja uruchomienia
Gra wymaga uruchomienia trzech niezależnych instancji aplikacji w osobnych oknach terminala. Dzięki konfiguracji w pliku pom.xml uruchamianie odbywa się za pomocą jednej, skróconej komendy.

**Krok 1:** Uruchomienie Serwera Otwórz pierwszy terminal i wpisz poniższą komendę. Aplikacja wykryje wolny port i wystartuje jako Serwer.
````
mvn exec:java -Dexec.args="server"
````
**Krok 2:** Dołączenie Gracza 1 (Czarne kamienie) Otwórz drugi terminal i wpisz tę samą komendę. Aplikacja wykryje działający serwer i połączy się jako pierwszy klient.
```
mvn exec:java
```
**Krok 3:** Dołączenie Gracza 2 (Białe kamienie) Otwórz trzeci terminal i wpisz komendę po raz ostatni. Aplikacja dołączy jako drugi klient i rozgrywka się rozpocznie.

```
mvn exec:java
```
### Sterowanie i komendy
Gra obsługiwana jest z poziomu konsoli tekstowej.

Wykonywanie ruchów: Współrzędne należy podawać w formacie [Litera][Liczba]. Wielkość liter nie ma znaczenia. Przykłady:
- D13
- a1
- H10

Komendy specjalne:

**pass** - gracz pasuje (oddaje ruch bez stawiania kamienia).\
**surrender** - gracz poddaje partię.\
**quit** - wyjście z gry (rozłączenie).