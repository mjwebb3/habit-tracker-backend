package com.tp1.habittracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tp1.habittracker.config.HabitSimilarityProperties;
import com.tp1.habittracker.domain.enums.Frequency;
import com.tp1.habittracker.domain.enums.HabitType;
import com.tp1.habittracker.domain.model.Habit;
import com.tp1.habittracker.repository.HabitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HabitSimilarityServiceTest {

    private OllamaClient ollamaClient;
    private HabitRepository habitRepository;
    private HabitSimilarityProperties properties;
    private HabitSimilarityService service;

    @BeforeEach
    void setUp() {
        ollamaClient = mock(OllamaClient.class);
        habitRepository = mock(HabitRepository.class);
        properties = new HabitSimilarityProperties();
        properties.setSimilarityThreshold(0.8);
        service = new HabitSimilarityService(ollamaClient, habitRepository, properties);
    }

    @Test
    void findMostSimilarHabitReturnsBestMatchWhenSimilarityAboveThreshold() {
        // Query embedding
        List<Double> queryEmbedding = List.of(1.0, 0.0, 0.0);

        // Habit with high similarity
        List<Double> highSimilarityEmbedding = List.of(0.95, 0.1, 0.0);
        Habit similarHabit = Habit.builder()
                .id("habit-1")
                .userId("user-1")
                .name("Drink water")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(highSimilarityEmbedding)
                .build();

        // Habit with low similarity
        List<Double> lowSimilarityEmbedding = List.of(0.0, 1.0, 0.0);
        Habit dissimilarHabit = Habit.builder()
                .id("habit-2")
                .userId("user-1")
                .name("Do yoga")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(lowSimilarityEmbedding)
                .build();

        when(ollamaClient.generateEmbedding("Drink water")).thenReturn(queryEmbedding);
        when(habitRepository.findAll()).thenReturn(List.of(similarHabit, dissimilarHabit));

        Optional<Habit> result = service.findMostSimilarHabit("Drink water");

        assertTrue(result.isPresent());
        assertEquals("habit-1", result.get().getId());
    }

    @Test
    void findMostSimilarHabitReturnsEmptyWhenBestMatchBelowThreshold() {
        // Query embedding
        List<Double> queryEmbedding = List.of(1.0, 0.0, 0.0);

        // Habit with similarity below threshold
        List<Double> lowSimilarityEmbedding = List.of(0.0, 0.9, 0.1);
        Habit dissimilarHabit = Habit.builder()
                .id("habit-1")
                .userId("user-1")
                .name("Do yoga")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(lowSimilarityEmbedding)
                .build();

        when(ollamaClient.generateEmbedding("Meditate")).thenReturn(queryEmbedding);
        when(habitRepository.findAll()).thenReturn(List.of(dissimilarHabit));

        Optional<Habit> result = service.findMostSimilarHabit("Meditate");

        assertTrue(result.isEmpty());
    }

    @Test
    void findMostSimilarHabitIgnoresHabitsWithNullEmbedding() {
        // Query embedding
        List<Double> queryEmbedding = List.of(1.0, 0.0, 0.0);

        // Habit with null embedding
        Habit habitWithNullEmbedding = Habit.builder()
                .id("habit-1")
                .userId("user-1")
                .name("Old habit")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(null)
                .build();

        // Habit with valid embedding above threshold
        List<Double> validEmbedding = List.of(0.95, 0.1, 0.0);
        Habit validHabit = Habit.builder()
                .id("habit-2")
                .userId("user-1")
                .name("Drink water")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(validEmbedding)
                .build();

        when(ollamaClient.generateEmbedding("Drink water")).thenReturn(queryEmbedding);
        when(habitRepository.findAll()).thenReturn(List.of(habitWithNullEmbedding, validHabit));

        Optional<Habit> result = service.findMostSimilarHabit("Drink water");

        assertTrue(result.isPresent());
        assertEquals("habit-2", result.get().getId());
    }

    @Test
    void findMostSimilarHabitIgnoresHabitsWithEmptyEmbedding() {
        // Query embedding
        List<Double> queryEmbedding = List.of(1.0, 0.0, 0.0);

        // Habit with empty embedding
        Habit habitWithEmptyEmbedding = Habit.builder()
                .id("habit-1")
                .userId("user-1")
                .name("Empty habit")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(List.of())
                .build();

        // Habit with valid embedding above threshold
        List<Double> validEmbedding = List.of(0.95, 0.1, 0.0);
        Habit validHabit = Habit.builder()
                .id("habit-2")
                .userId("user-1")
                .name("Drink water")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(validEmbedding)
                .build();

        when(ollamaClient.generateEmbedding("Drink water")).thenReturn(queryEmbedding);
        when(habitRepository.findAll()).thenReturn(List.of(habitWithEmptyEmbedding, validHabit));

        Optional<Habit> result = service.findMostSimilarHabit("Drink water");

        assertTrue(result.isPresent());
        assertEquals("habit-2", result.get().getId());
    }

    @Test
    void findMostSimilarHabitReturnsEmptyWhenBlankInput() {
        Optional<Habit> result = service.findMostSimilarHabit("   ");

        assertTrue(result.isEmpty());
    }

    @Test
    void findMostSimilarHabitReturnsEmptyWhenNoHabitsExist() {
        // Query embedding
        List<Double> queryEmbedding = List.of(1.0, 0.0, 0.0);

        when(ollamaClient.generateEmbedding("Drink water")).thenReturn(queryEmbedding);
        when(habitRepository.findAll()).thenReturn(List.of());

        Optional<Habit> result = service.findMostSimilarHabit("Drink water");

        assertTrue(result.isEmpty());
    }

    @Test
    void findMostSimilarHabitSelectsBestMatchWhenMultipleAboveThreshold() {
        // Query embedding
        List<Double> queryEmbedding = List.of(1.0, 0.0, 0.0, 0.0);

        // Habit 1 with similarity ~0.95 (dot product ~0.95, magnitudes ~1.0)
        List<Double> embedding1 = List.of(0.95, 0.1, 0.0, 0.0);

        // Habit 2 with similarity ~0.87 (dot product ~0.87, magnitudes ~1.0)
        List<Double> embedding2 = List.of(0.87, 0.3, 0.35, 0.0);

        Habit habit1 = Habit.builder()
                .id("habit-1")
                .userId("user-1")
                .name("Drink water")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(embedding1)
                .build();

        Habit habit2 = Habit.builder()
                .id("habit-2")
                .userId("user-1")
                .name("Hydration")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(embedding2)
                .build();

        when(ollamaClient.generateEmbedding("Drink water")).thenReturn(queryEmbedding);
        when(habitRepository.findAll()).thenReturn(List.of(habit1, habit2));

        Optional<Habit> result = service.findMostSimilarHabit("Drink water");

        assertTrue(result.isPresent());
        assertEquals("habit-1", result.get().getId());
    }

    @Test
    void findMostSimilarHabitThrowsWhenNewHabitNameIsNull() {
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> service.findMostSimilarHabit(null)
        );
    }

    @Test
    void findMostSimilarHabitReturnsEmptyWhenAllHabitsHaveBadEmbeddings() {
        // Query embedding
        List<Double> queryEmbedding = List.of(1.0, 0.0, 0.0);

        // Habit with mismatched embedding dimensions (will cause IllegalArgumentException)
        List<Double> badEmbedding = List.of(1.0, 0.0); // Missing third dimension
        Habit badHabit = Habit.builder()
                .id("habit-1")
                .userId("user-1")
                .name("Bad habit")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now())
                .embedding(badEmbedding)
                .build();

        when(ollamaClient.generateEmbedding("Drink water")).thenReturn(queryEmbedding);
        when(habitRepository.findAll()).thenReturn(List.of(badHabit));

        Optional<Habit> result = service.findMostSimilarHabit("Drink water");

        assertTrue(result.isEmpty());
    }
}
