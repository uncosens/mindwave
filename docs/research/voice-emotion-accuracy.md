# How long should the anchor reading be?

Research notes for Mindwave's daily anchor reading and baseline.
Written 2026-10-30. Plain summaries; please check the original papers before
quoting numbers elsewhere.

## Short answer

- The old anchor ("Every morning I walk slowly to the river and back.") is
  about 3 s and 13 syllables. That is too short for most of our features.
- Mean and median pitch need about **10 s** of reading to settle.
  Shimmer and other voice-quality measures need about **20 s / 40-50 words**.
- **Recommendation: an anchor of about 15-20 s of reading, about 55-65 words
  or 70-80 syllables, in 4-5 short sentences.** This is the shortest length
  where pitch, loudness, rate and pauses are reasonably stable, while still
  fitting into a daily check-in (and 3 readings at calibration, about 1 min).
- The new anchor in `Sentences.kt` has 62 words, 74 syllables and 5
  sentences, about 17-19 s at a normal reading pace.

## 1. How much speech each feature needs

| Feature in Mindwave | What the research says | Source |
|---|---|---|
| Median F0 (`pitchMean`) | Long-term mean and median F0 stabilise after about 10 s of read text; the F0 "base value" after about 5 s. After that point the variance drops about 40-fold. | Arantes & Eriksson 2014 |
| F0 variability (`pitchVar`) | In connected speech, samples of 30 s or more were needed to capture each child's pitch variability (mean sample 36 s). Variability needs more speech than the mean. | AJSLP 2014 study of school-age children |
| Jitter | Cycle-level jitter needed up to 190 glottal cycles to level off; shimmer up to 130 cycles. Disordered voices need longer windows. | Karnell 1991 |
| Shimmer, HNR, CPPS, AVQI | Connected-speech voice-quality measures reached internal consistency at about 20 s / 40-50 words of read speech. Samples under about 10 s / 20 words often drifted away from the speaker's own median and tended to underestimate. The often-quoted "3 s of voiced speech" rule had weak support. Spectral slope / tilt settled after only about 5 s. | Nylén 2025 |
| Speech rate, pauses | On consumer devices, F0 and CPP were reliable across days and devices; timing measures such as syllable rate were clearly less reliable. | Cross-device study, Behavior Research Methods 2024/25 |
| Jitter / shimmer on phones | Smartphone jitter and shimmer are sensitive to background noise and room reverberation; F0 and spectral measures hold up better. | QMU thesis; UCL preprint 2023 |

Notes for our setup:

- Our jitter and shimmer are frame-level proxies (20 ms hop), not
  cycle-level. They are probably even less stable than clinical jitter, so
  they need at least as much speech.
- At about 120 Hz (typical male F0), 190 cycles is only about 1.6 s of
  voiced speech, but that is for a sustained vowel. In running speech,
  voiced stretches are short and broken by consonants and pauses, so you
  need much more total time (this is why Nylén finds about 20 s).
- More voiced sounds per second means more usable pitch frames. That is why
  the new anchor favours voiced consonants (w, r, l, m, n, v, b, d, g, z).

## 2. Day-to-day stability (test-retest)

- Healthy voices change with time of day: in 45 healthy young women, voices
  were "worse" in the morning, but did not differ across consecutive days
  when time of day was held constant (Pierce et al. 2021).
- Normal intrasubject variability is real and should not be read as mood
  change. Higgins et al. (1994) derived cut-offs for normal session-to-session
  variation; Awan et al. (2012) found jitter and soft-voice measures the least
  stable over 1-2 weeks; Bough et al. (1996) found weak links with sleep,
  hydration and mood.
- In a sustained-vowel study, at least four repeated productions were needed
  for acceptable reliability of all acoustic measures (USM study). This
  supports keeping several calibration readings plus a rolling window.

## 3. Standard reading materials

Durations below are estimates at a normal reading rate (about 160-180 words
per minute, about 4-5 syllables per second). Word counts are approximate.

| Material | Length | Typical reading time | Purpose |
|---|---|---|---|
| CAPE-V sentences (Kempster et al. 2009) | 6 short sentences | about 2-4 s each | Each sentence targets one context: all-voiced, vowels, easy onset with /h/, voiceless stops, nasals, hard glottal attacks. Used with sustained vowels and about 20 s of free speech. |
| "Please Call Stella" (Speech Accent Archive, Weinberger) | 69 words | about 20-25 s | Covers nearly all English sounds in one short paragraph. |
| "The North Wind and the Sun" (IPA Handbook, 1999) | about 110 words | about 35-45 s | Standard phonetic sample across languages. Used in the 26-language F0 study above. |
| "Grandfather Passage" (Darley et al. 1975) | about 130 words | about 40-50 s | Motor speech / dysarthria testing. |
| "Rainbow Passage" (Fairbanks 1960) | about 330 words | about 1.5-2 min; often only the first paragraph (about 20-30 s) is used | Phonetically balanced; voice and accent work. |

Takeaways:

- Research and clinics mostly use **20 s or more** of reading for voice
  measures. Our old 3 s sentence is closer to one CAPE-V sentence, which is
  meant for listening, not for acoustic averages.
- Short, targeted sentences (CAPE-V idea) are useful *inside* a longer text:
  an all-voiced sentence gives clean pitch frames.
- We did not reuse these passages. Their copyright status varies and some
  users may know them by heart. The new anchor is an original text.

## 4. Emotional neutrality of the text

- The words we read change how we sound. Read sentences grouped by
  emotional valence produce measurably different speech, and depressed
  speakers show more hesitations and errors on affective sentences
  (Stasak, Epps & Goecke 2019). For a *baseline*, the text should therefore
  carry no strong emotion, so that any change comes from the speaker.
- Word-level norms exist for checking this: Warriner, Kuperman & Brysbaert
  (2013) rate about 14,000 English words for valence, arousal and dominance.
  The new anchor uses everyday nouns and verbs (road, hills, lane, bridge,
  farmer, beans, water, home) and avoids strongly positive or negative words.
  A few words ("mild", "green", "garden") are mildly pleasant, as in most
  neutral texts.
- The anchor text must stay **exactly the same** every day. Changing it
  changes rate, pauses and accuracy, which is why those metrics are not
  marked `textRobust`.

## 5. Design of the new anchor

```
We were away all day, and we rode along the valley road.
The morning air was mild, and the low hills were green.
Near the old mill, a narrow lane winds over a small bridge.
Beyond it, a farmer keeps bees, a few goats, and a garden of beans and peas.
We walked back by the water and reached home before evening.
```

- 62 words, 74 syllables, 5 sentences: about 17-19 s.
- Sentence 1 is fully voiced (like the CAPE-V all-voiced sentence) and
  sentence 2 nearly so, giving a long run of clean pitch frames early on.
- Sentences 3-5 add voiceless stops and fricatives (p, t, k, f, s, h, ch),
  nasals and glides for phonetic balance and a fair test of articulation.
- Four sentence breaks give predictable pause positions, so `pauseRatio`
  compares like with like across days.
- Simple, common words keep the Vosk word error rate low and stable.
- Note: `TextTools.syllables` counts vowel groups, so it reads this text as
  a slightly different number (for example "beyond" as 1, "walked" as 2).
  This bias is constant for a fixed text, so z-scores are not affected.

## 6. Further ways to improve accuracy (ranked, not implemented)

1. **Learn per-user weights from the mood ratings.** The arousal and valence
   weights in `Analyzer` are hand-set. After about 4-6 weeks of check-ins,
   fit a small regularised regression per user from metric z-scores to the
   1-5 mood rating. Speaker-dependent models clearly beat generic ones in
   emotion recognition (Rahman & Busso 2012; Busso et al. 2013).
2. **Gate on signal quality.** Measure the noise floor and SNR before speech
   and require a minimum amount of voiced speech (for example 6-8 s for the
   new anchor). Ask for a retry instead of storing a bad sample. Noise and
   rooms mostly hurt jitter, shimmer and loudness (QMU thesis; UCL 2023).
3. **Replace or down-weight the jitter/shimmer proxies.** Cepstral peak
   prominence (CPP/CPPS) is far more reliable across phones and days
   (Behavior Research Methods 2024/25; Nylén 2025), and spectral slope or
   alpha ratio settle in about 5 s. These are in GeMAPS (Eyben et al. 2016),
   the standard minimal feature set for affect research.
4. **Control for time of day.** Store the check-in time and either suggest a
   fixed time or compare only with baseline readings from a similar part of
   the day (Pierce et al. 2021).
5. **Better speech rate.** Use Vosk word timestamps for articulation rate
   (syllables per second of phonation, pauses removed) instead of the
   vowel-group counter and an energy VAD. Timing measures are the weakest on
   phones, so they gain most from better segmentation.
6. **Normalise the baseline more carefully.** Keep median/MAD (robust, good),
   but only trust a metric once there are about 5-7 baseline points, and
   drop baseline samples that were flagged as low quality or very unusual.
   This follows the idea of normalising each speaker against their own
   neutral speech (Busso et al. 2011, 2013).
7. **Do not mix text lengths in one baseline.** `textRobust` metrics compare
   the short daily sentence (3-4 s) against the long anchor baseline. Pitch
   declines over a long utterance, so short and long readings may differ
   slightly. Track the daily sentence's own baseline for all metrics once
   enough samples exist.
8. **Add a few more GeMAPS-style features.** F0 20th-80th percentile range,
   loudness variability and MFCC summary values. MFCCs carried the most
   information in recent smartphone depression studies (Frontiers in Public
   Health 2025) but are hard to explain to users, so add them only if a
   learned model (item 1) is used.
9. **Thin-slicing.** For depression, the start of each read sentence was more
   informative than the whole reading (Alghowinem et al. 2013). Worth testing
   per-sentence features once data exist.

## References

- Alghowinem, S., Goecke, R., Wagner, M., Epps, J., Breakspear, M., Parker, G. (2013). Detecting depression: a comparison between spontaneous and read speech. *ICASSP 2013*, 7547-7551. https://researchprofiles.canberra.edu.au/en/publications/detecting-depression-a-comparison-between-spontaneous-and-read-sp/
- Arantes, P., Eriksson, A. (2014). Temporal stability of long-term measures of fundamental frequency. *Speech Prosody 2014*, 1149-1152. https://doi.org/10.21437/SpeechProsody.2014-220
- Awan, S. N., Miesemer, S. A., Nicolia, T. A. (2012). An examination of intrasubject variability on the Dysphonia Severity Index. *Journal of Voice*. https://read.qxmd.com/read/22921294/an-examination-of-intrasubject-variability-on-the-dysphonia-severity-index
- Bough et al. (1996). Intrasubject variability of objective voice measures. *Journal of Voice*. https://researchdiscovery.drexel.edu/esploro/outputs/journalArticle/Intrasubject-variability-of-objective-voice-measures/991019312432004721
- Busso, C. et al. (2011). Iterative feature normalization for emotional speech detection. *ICASSP 2011*. https://msp.utdallas.edu/publications/Busso_2011.pdf
- Busso, C. et al. (2013). Iterative feature normalization scheme for automatic emotion detection from speech. *IEEE Transactions on Affective Computing*. https://msp.utdallas.edu/publications/Busso_2013_2.pdf
- Cross-device and test-retest reliability of speech acoustic measurements derived from consumer-grade mobile recording devices (2024/2025). *Behavior Research Methods*. https://doi.org/10.3758/s13428-024-02584-0 (summary: https://english.cas.cn/newsroom/research_news/phys/202502/t20250212_901407.shtml)
- Cummins, N., Scherer, S., Krajewski, J., Schnieder, S., Epps, J., Quatieri, T. F. (2015). A review of depression and suicide risk assessment using speech analysis. *Speech Communication*, 71, 10-49. https://dblp.org/rec/journals/speech/CumminsSKSEQ15.html
- Eyben, F. et al. (2016). The Geneva Minimalistic Acoustic Parameter Set (GeMAPS) for voice research and affective computing. *IEEE Transactions on Affective Computing*, 7(2), 190-202. https://doi.org/10.1109/TAFFC.2015.2457417
- (2014). Speaking fundamental frequency and individual variability in Caucasian and African American school-age children. *American Journal of Speech-Language Pathology*. https://pubs.asha.org/doi/10.1044/2014_AJSLP-13-0016
- Higgins, M. B., Netsell, R., Schulte, L. (1994). Study of normal intrasubject variability of phonatory measures within and across sessions. *JSHR*, 37(1), 38-45. https://pubs.asha.org/doi/epdf/10.1044/jshr.3701.38
- Karnell, M. P. (1991). Laryngeal perturbation analysis: minimum length of analysis window. *Journal of Speech and Hearing Research*, 34(3), 544-548. https://doi.org/10.1044/jshr.3403.544
- Kempster, G. B., Gerratt, B. R., Verdolini Abbott, K., Barkmeier-Kraemer, J., Hillman, R. E. (2009). Consensus auditory-perceptual evaluation of voice: development of a standardized clinical protocol. *American Journal of Speech-Language Pathology*, 18, 124-132. https://talkbank.org/motor/access/0docs/CAPE-V/Kempster25.pdf
- Low, D. M., Bentley, K. H., Ghosh, S. S. (2020). Automated assessment of psychiatric disorders using speech: a systematic review. *Laryngoscope Investigative Otolaryngology*, 5(1), 96-116. https://pmc.ncbi.nlm.nih.gov/articles/PMC7042657
- Nylén, F. (2025). On Acoustic Voice Quality Index measurement reliability in digital health applications: a narrative review and empirical evaluation of speech sample length requirements. *Frontiers in Digital Health*, 7. https://www.frontiersin.org/journals/digital-health/articles/10.3389/fdgth.2025.1610353/full
- Pierce et al. (2021). Acoustic variability in the healthy female voice within and across days: how much and why? *JSLHR*. https://pubs.asha.org/doi/10.1044/2021_JSLHR-21-00018
- Rahman, T., Busso, C. (2012). Personalized emotion recognition with iterative feature normalization. https://msp.utdallas.edu/publications/Rahman_2012.pdf
- Smartphone depression detection from neutral reading in university students (2025). *Frontiers in Public Health*. https://public-pages-files-2025.frontiersin.org/journals/public-health/articles/10.3389/fpubh.2025.1561332/text
- Stasak, B., Epps, J., Goecke, R. (2019). Automatic depression classification based on affective read sentences. *Speech Communication*. https://doi.org/10.1016/j.specom.2019.10.003
- Warriner, A. B., Kuperman, V., Brysbaert, M. (2013). Norms of valence, arousal, and dominance for 13,915 English lemmas. *Behavior Research Methods*, 45(4), 1191-1207. https://doi.org/10.3758/s13428-012-0314-x
- Reliability of acoustic parameters measurement from different trials of voice sample production (USM). https://libmast.utm.my/Record/usm.eprints-58311/Details
- Smartphone voice quality under noise (Queen Margaret University thesis). https://eresearch.qmu.ac.uk/items/a5b02fc7-1a6a-4e4f-a1f1-8f5e3d201773/full
- Effect of reverberation and device on voice markers (UCL preprint, 2023). https://discovery.ucl.ac.uk/10181921/1/2305.12514.pdf
- Reading passages: Speech Accent Archive (https://accent.gmu.edu), IPA (1999) *Handbook of the International Phonetic Association*, Fairbanks (1960) *Voice and Articulation Drillbook*, Darley, Aronson & Brown (1975) *Motor Speech Disorders*.
