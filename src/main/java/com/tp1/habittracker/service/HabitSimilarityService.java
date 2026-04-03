package com.tp1.habittracker.service;

import com.tp1.habittracker.config.HabitSimilarityProperties;
import com.tp1.habittracker.domain.model.Habit;
import com.tp1.habittracker.repository.HabitRepository;
import com.tp1.habittracker.util.SimilarityUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HabitSimilarityService {

    private final OllamaClient ollamaClient;
    private final HabitRepository habitRepository;
    private final HabitSimilarityProperties properties;

    /**
     * Finds the most similar habit to a new habit name using cosine similarity of embeddings.
     *
     * @param newHabitName the name of the new habit
     * @return an Optional containing the most similar habit if one is found above the similarity threshold,
     *         otherwise empty
     */
    public Optional<Habit> findMostSimilarHabit(String newHabitName) {
        Objects.requireNonNull(newHabitName, "newHabitName must not be null");

        if (newHabitName.isBlank()) {
            return Optional.empty();
        }

        // Generate embedding for the new habit name
        List<Double> newHabitEmbedding = ollamaClient.generateEmbedding(newHabitName);

        // Retrieve all habits
        List<Habit> allHabits = habitRepository.findAll();

        double highestSimilarity = -1.0;
        Habit mostSimilarHabit = null;

        // Find the habit with the highest similarity
        for (Habit habit : allHabits) {
            // Skip habits with null or empty embeddings
            if (habit.getEmbedding() == null || habit.getEmbedding().isEmpty()) {
                continue;
            }

            try {
                double similarity = SimilarityUtils.cosineSimilarity(
                        newHabitEmbedding,
                        habit.getEmbedding()
                );

                if (similarity > highestSimilarity) {
                    highestSimilarity = similarity;
                    mostSimilarHabit = habit;
                }
            } catch (IllegalArgumentException ex) {
                // Skip habits that cause similarity computation errors
                continue;
            }
        }

        // Return the habit only if similarity meets the threshold
        if (highestSimilarity >= properties.getSimilarityThreshold()) {
            return Optional.ofNullable(mostSimilarHabit);
        }

        return Optional.empty();
    }
}
