package com.app.ideaforge.generation;

import java.util.List;

import com.app.ideaforge.generation.dto.IdeationResponse;
import com.app.ideaforge.generation.dto.ScoringResponse;
import com.app.ideaforge.ideas.dto.Competitor;
import com.app.ideaforge.ideas.dto.IdeaContent;
import com.app.ideaforge.ideas.dto.ScoreRationales;

/** Valid LLM response fixtures for pipeline tests. */
public final class TestIdeas {

	private TestIdeas() {
	}

	public static IdeaContent idea(String title) {
		return new IdeaContent(
				title,
				"Tagline for " + title,
				"Summary for " + title,
				new IdeaContent.Problem(
						"Clinics lose revenue to missed appointments",
						"Practice managers",
						"Phone tag and paper lists",
						"high - direct revenue loss",
						"weekly per provider"),
				new IdeaContent.TargetCustomer(
						"Independent practice manager",
						"Small clinics (1-50)",
						"Small business",
						"Owner or practice manager"),
				new IdeaContent.Solution(
						"Automated scheduling and reminders",
						List.of(
								new IdeaContent.KeyFeature("Smart reminders", "SMS and email cadence"),
								new IdeaContent.KeyFeature("Waitlist fill", "Backfill cancellations"),
								new IdeaContent.KeyFeature("No-show scoring", "Predict likely no-shows")),
						List.of("Reminder scheduling", "Waitlist", "Basic dashboard")),
				new IdeaContent.AiRole(
						"Predicts no-show risk from history",
						List.of("Forecasting", "Text/LLM"),
						"Without prediction the reminders are uniform and miss the risky patients",
						"Appointment history and outcomes",
						"Biased history can mispredict; explain scores to staff"),
				new IdeaContent.Differentiation(
						"Built for independent clinics, not hospital systems",
						"Patient outcome data improves predictions",
						"Practice management APIs are now widely available"),
				new IdeaContent.Monetization(
						"Subscription",
						"$99/month per location",
						"One recovered appointment pays for the month"),
				new IdeaContent.GoToMarket(
						"Local dental associations",
						List.of("Association partnerships", "SEO for scheduling terms")),
				new IdeaContent.BuildEstimate(
						"medium",
						"6-8 weeks",
						"One full-stack developer"),
				List.of("Data quality varies", "Reminder fatigue", "Integration coverage"),
				List.of("HIPAA: a BAA and encrypted storage would be required"),
				List.of("Which practice management systems expose APIs?"));
	}

	public static IdeationResponse ideation(String... titles) {
		return new IdeationResponse(java.util.Arrays.stream(titles).map(TestIdeas::idea).toList());
	}

	public static com.app.ideaforge.generation.dto.GenerationParameters parameters() {
		return com.app.ideaforge.generation.dto.GenerationParameters.defaultParameters();
	}

	public static ScoringResponse strongScores(int count) {
		List<ScoringResponse.ScoredIdea> scores = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			scores.add(strongScore(i));
		}
		return new ScoringResponse(scores);
	}

	public static ScoringResponse scoresWithWeakIdea(int count, int weakIndex) {
		List<ScoringResponse.ScoredIdea> scores = new java.util.ArrayList<>();
		for (int i = 0; i < count; i++) {
			if (i == weakIndex) {
				scores.add(new ScoringResponse.ScoredIdea(
						i, 2, 5, 9, 2,
						true,
						new ScoreRationales("Almost no demand signals", "Weak budgets", "Saturated market", "Done many times"),
						List.of(new Competitor("Giant", "https://example.com", "Well funded horizontal"))));
			} else {
				scores.add(strongScore(i));
			}
		}
		return new ScoringResponse(scores);
	}

	private static ScoringResponse.ScoredIdea strongScore(int index) {
		return new ScoringResponse.ScoredIdea(
				index, 7 + (index % 3), 7, 4 + (index % 3), 7,
				true,
				new ScoreRationales("Demand evidence [0]", "Pricing supports margin", "Crowded but vertical gaps", "Fresh wedge"),
				List.of(new Competitor("Incumbent", "https://example.com", "Broad horizontal tool")));
	}
}
