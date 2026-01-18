package go.si;

import java.awt.*;

//rekord przechowujacy dane o punkcie oraz przypisanej do niego wartosci numerycznej (zastosowanie DTO)
public record CandidateRecord(
        Point point,
        double score
) {
}
