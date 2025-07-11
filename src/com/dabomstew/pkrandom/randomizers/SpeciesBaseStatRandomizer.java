package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkromio.constants.SpeciesIDs;
import com.dabomstew.pkromio.gamedata.ExpCurve;
import com.dabomstew.pkromio.gamedata.MegaEvolution;
import com.dabomstew.pkromio.gamedata.Species;
import com.dabomstew.pkromio.gamedata.SpeciesSet;
import com.dabomstew.pkromio.gamedata.cueh.BasicSpeciesAction;
import com.dabomstew.pkromio.gamedata.cueh.EvolvedSpeciesAction;
import com.dabomstew.pkromio.romhandlers.RomHandler;

import java.util.*;

public class SpeciesBaseStatRandomizer extends Randomizer {

    private static final int SHEDINJA_HP = 1;
    protected static final int MIN_HP = 20;
    protected static final int MIN_NON_HP_STAT = 10;
    private static final int CHANSEY_CATCH_RATE_GEN5 = 608;
    private static final int MAX_HEIGHT = 999;
    private static final int MAX_WEIGHT = 9990;

    public SpeciesBaseStatRandomizer(RomHandler romHandler, Settings settings, Random random) {
        super(romHandler, settings, random);
    }

    Map<Species, List<Integer>> shuffledStatsOrders;

    public void shuffleSpeciesStats() {
        boolean evolutionSanity = settings.isBaseStatsFollowEvolutions();
        boolean megaEvolutionSanity = settings.isBaseStatsFollowMegaEvolutions();

        shuffledStatsOrders = new HashMap<>();

        copyUpEvolutionsHelper.apply(evolutionSanity, false,
                this::putShuffledStatsOrder, this::copyUpShuffledStatsOrder);

        romHandler.getSpeciesSetInclFormes().filter(Species::isActuallyCosmetic)
                .forEach(pk -> copyUpShuffledStatsOrder(pk.getBaseForme(), pk));

        if (megaEvolutionSanity) {
            for (MegaEvolution megaEvo : romHandler.getMegaEvolutions()) {
                if (megaEvo.getFrom().getMegaEvolutionsFrom().size() == 1) {
                    copyUpShuffledStatsOrder(megaEvo.getFrom(), megaEvo.getTo());
                }
            }
        }

        romHandler.getSpeciesSetInclFormes().forEach(this::applyShuffledOrderToStats);

        changesMade = true;
    }

    protected void putShuffledStatsOrder(Species pk) {
        List<Integer> order = Arrays.asList(0, 1, 2, 3, 4, 5);
        Collections.shuffle(order, random);
        shuffledStatsOrders.put(pk, order);
    }

    private void copyUpShuffledStatsOrder(Species from, Species to) {
        copyUpShuffledStatsOrder(from, to, false);
    }

    private void copyUpShuffledStatsOrder(Species from, Species to, boolean unused) {
        // has a third boolean parameter just so it can fit the EvolvedSpeciesAction functional interface
        shuffledStatsOrders.put(to, shuffledStatsOrders.get(from));
    }

    protected void applyShuffledOrderToStats(Species pk) {
        if (shuffledStatsOrders.containsKey(pk)) {
            List<Integer> order = shuffledStatsOrders.get(pk);
            List<Integer> stats = Arrays.asList(
                    pk.getHp(), pk.getAttack(), pk.getDefense(), pk.getSpatk(), pk.getSpdef(), pk.getSpeed()
            );
            pk.setHp(stats.get(order.get(0)));
            pk.setAttack(stats.get(order.get(1)));
            pk.setDefense(stats.get(order.get(2)));
            pk.setSpatk(stats.get(order.get(3)));
            pk.setSpdef(stats.get(order.get(4)));
            pk.setSpeed(stats.get(order.get(5)));
        }
    }

    private BasicSpeciesAction<Species> evaluateBpActions(boolean randomizeStatsCompletely) {
        if (randomizeStatsCompletely) {
            return this::randomizeStatsCompletely;
        } else {
            return this::randomizeStatsWithinBST;
        }
    }

    public void randomizeSpeciesStats() {
        boolean evolutionSanity = settings.isBaseStatsFollowEvolutions();
        boolean megaEvolutionSanity = settings.isBaseStatsFollowMegaEvolutions();
        boolean assignEvoStatsRandomly = settings.isAssignEvoStatsRandomly();
        boolean randomizeStatsCompletely = settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.RANDOM_COMPLETELY;

        BasicSpeciesAction<Species> bpAction = evaluateBpActions(randomizeStatsCompletely);
        EvolvedSpeciesAction<Species> randomEpAction = (evFrom, evTo, toMonIsFinalEvo) ->
                assignNewStatsForEvolution(evFrom, evTo);
        EvolvedSpeciesAction<Species> copyEpAction = (evFrom, evTo, toMonIsFinalEvo) ->
                copyRandomizedStatsUpEvolution(evFrom, evTo);

        copyUpEvolutionsHelper.apply(evolutionSanity, true, bpAction,
                assignEvoStatsRandomly ? randomEpAction : copyEpAction, randomEpAction, bpAction);

        romHandler.getSpeciesSetInclFormes().filter(Species::isActuallyCosmetic)
                .forEach(pk -> pk.copyBaseFormeBaseStats(pk.getBaseForme()));

        if (megaEvolutionSanity) {
            for (MegaEvolution megaEvo : romHandler.getMegaEvolutions()) {
                if (megaEvo.getFrom().getMegaEvolutionsFrom().size() > 1 || assignEvoStatsRandomly) {
                    assignNewStatsForEvolution(megaEvo.getFrom(), megaEvo.getTo());
                } else {
                    copyRandomizedStatsUpEvolution(megaEvo.getFrom(), megaEvo.getTo());
                }
            }
        }
        changesMade = true;
    }

    int randomBS(double baseStatWeight) {
        return (int) Math.round(baseStatWeight * 254) + 1;
    }

    protected void randomizeStatsCompletely(Species pk) {
        double hpW = random.nextDouble(), atkW = random.nextDouble(), defW = random.nextDouble(),
                speW = random.nextDouble(), spaW = random.nextDouble(), spdW = random.nextDouble();

        pk.setHp(randomBS(hpW));
        pk.setAttack(randomBS(atkW));
        pk.setDefense(randomBS(defW));
        pk.setSpeed(randomBS(speW));
        pk.setSpatk(randomBS(spaW));
        pk.setSpdef(randomBS(spdW));
    }

    protected void randomizeStatsWithinBST(Species pk) {
        do {
            if (pk.getNumber() == SpeciesIDs.shedinja) {
                randomizeShedinjaStatsWithinBST(pk);
            } else {
                randomizeRegularStatsWithinBST(pk);
            }
            // Re-roll if the stats become something we can't store
        } while (pk.getHp() > 255 || pk.getAttack() > 255 || pk.getDefense() > 255 || pk.getSpecial() > 255
                || pk.getSpeed() > 255);
    }

    private void randomizeShedinjaStatsWithinBST(Species pk) {
        // Shedinja is horribly broken unless we restrict it to 1HP.
        int bst = pk.getBST() - (SHEDINJA_HP + MIN_NON_HP_STAT * 5);

        // Make weightings
        double atkW = random.nextDouble(), defW = random.nextDouble();
        double spaW = random.nextDouble(), spdW = random.nextDouble(), speW = random.nextDouble();

        double totW = atkW + defW + spaW + spdW + speW;

        pk.setHp(SHEDINJA_HP);
        pk.setAttack((int) Math.max(1, Math.round(atkW / totW * bst)) + MIN_NON_HP_STAT);
        pk.setDefense((int) Math.max(1, Math.round(defW / totW * bst)) + MIN_NON_HP_STAT);
        pk.setSpatk((int) Math.max(1, Math.round(spaW / totW * bst)) + MIN_NON_HP_STAT);
        pk.setSpdef((int) Math.max(1, Math.round(spdW / totW * bst)) + MIN_NON_HP_STAT);
        pk.setSpeed((int) Math.max(1, Math.round(speW / totW * bst)) + MIN_NON_HP_STAT);
    }

    private void randomizeRegularStatsWithinBST(Species pk) {
        int bst = pk.getBST() - (MIN_HP + MIN_NON_HP_STAT * 5);

        // Make weightings
        double hpW = random.nextDouble(), atkW = random.nextDouble(), defW = random.nextDouble();
        double spaW = random.nextDouble(), spdW = random.nextDouble(), speW = random.nextDouble();

        double totW = hpW + atkW + defW + spaW + spdW + speW;

        pk.setHp((int) Math.max(1, Math.round(hpW / totW * bst)) + MIN_HP);
        pk.setAttack((int) Math.max(1, Math.round(atkW / totW * bst)) + MIN_NON_HP_STAT);
        pk.setDefense((int) Math.max(1, Math.round(defW / totW * bst)) + MIN_NON_HP_STAT);
        pk.setSpatk((int) Math.max(1, Math.round(spaW / totW * bst)) + MIN_NON_HP_STAT);
        pk.setSpdef((int) Math.max(1, Math.round(spdW / totW * bst)) + MIN_NON_HP_STAT);
        pk.setSpeed((int) Math.max(1, Math.round(speW / totW * bst)) + MIN_NON_HP_STAT);
    }

    protected void assignNewStatsForEvolution(Species from, Species to) {
        double bstDiff = to.getBST() - from.getBST();

        // Make weightings
        double hpW = random.nextDouble(), atkW = random.nextDouble(), defW = random.nextDouble();
        double spaW = random.nextDouble(), spdW = random.nextDouble(), speW = random.nextDouble();

        double totW = hpW + atkW + defW + spaW + spdW + speW;

        double hpDiff = Math.round((hpW / totW) * bstDiff);
        double atkDiff = Math.round((atkW / totW) * bstDiff);
        double defDiff = Math.round((defW / totW) * bstDiff);
        double spaDiff = Math.round((spaW / totW) * bstDiff);
        double spdDiff = Math.round((spdW / totW) * bstDiff);
        double speDiff = Math.round((speW / totW) * bstDiff);

        to.setHp((int) Math.min(255, Math.max(1, from.getHp() + hpDiff)));
        to.setAttack((int) Math.min(255, Math.max(1, from.getAttack() + atkDiff)));
        to.setDefense((int) Math.min(255, Math.max(1, from.getDefense() + defDiff)));
        to.setDefense((int) Math.min(255, Math.max(1, from.getSpatk() + spaDiff)));
        to.setSpdef((int) Math.min(255, Math.max(1, from.getSpdef() + spdDiff)));
        to.setSpeed((int) Math.min(255, Math.max(1, from.getSpeed() + speDiff)));
    }

    protected void copyRandomizedStatsUpEvolution(Species from, Species to) {
        double bstRatio = (double) to.getBST() / (double) from.getBST();

        to.setHp((int) Math.min(255, Math.max(1, Math.round(from.getHp() * bstRatio))));
        to.setAttack((int) Math.min(255, Math.max(1, Math.round(from.getAttack() * bstRatio))));
        to.setDefense((int) Math.min(255, Math.max(1, Math.round(from.getDefense() * bstRatio))));
        to.setSpatk((int) Math.min(255, Math.max(1, Math.round(from.getSpatk() * bstRatio))));
        to.setSpdef((int) Math.min(255, Math.max(1, Math.round(from.getSpdef() * bstRatio))));
        to.setSpeed((int) Math.min(255, Math.max(1, Math.round(from.getSpeed() * bstRatio))));
    }

    private int randomEvYield(double evYieldWeight) {
        return (int) Math.round(evYieldWeight * 3);
    }

    public void randomizeEvYields() {
        if (settings.isRandomizeEVYields()) {
            for (Species poke : romHandler.getSpeciesSetInclFormes()) {
                double evHpYieldW = random.nextDouble(), evAttackYieldW = random.nextDouble(), evDefenseYieldW = random.nextDouble(),
                        evSpatkYieldW = random.nextDouble(), evSpdefYieldW = random.nextDouble(), evSpeedYieldW = random.nextDouble();

                poke.setEvHpYield(randomEvYield(evHpYieldW));
                poke.setEvAttackYield(randomEvYield(evAttackYieldW));
                poke.setEvDefenseYield(randomEvYield(evDefenseYieldW));
                poke.setEvSpatkYield(randomEvYield(evSpatkYieldW));
                poke.setEvSpdefYield(randomEvYield(evSpdefYieldW));
                poke.setEvSpeedYield(randomEvYield(evSpeedYieldW));
            }
        }
        changesMade = true;
    }

    public void randomizeCatchRate() {
        if (settings.isRandomizeCatchRate()) {
            for (Species poke : romHandler.getSpeciesSetInclFormes()) {
                double catchRateW = random.nextDouble();
                poke.setCatchRate(randomBS(catchRateW));
            }
        }
        changesMade = true;
    }

    public void randomizeBaseExpYield() {
        if (settings.isRandomizeBaseExpYield()) {
            int generation = romHandler.generationOfPokemon();
            int maxBaseExpYield;

            if (generation >= 1 && generation <= 4)
                // The max value for this field's data type is 255 since it is a u8 in these generations.
                maxBaseExpYield = 255;
            else if (generation >= 5 && generation <= 7)
                // Since the data type for this field changed to a u16 in Gen5 and beyond, the new max value is 65535.
                // This value is way too high considering the Pokemon with the highest value is held by Chansey with
                // 608. Chansey's yield is what I chose instead.
                maxBaseExpYield = CHANSEY_CATCH_RATE_GEN5;
            else
                throw new IllegalStateException("Invalid value for generation: " + generation);

            for (Species poke : romHandler.getSpeciesSetInclFormes()) {
                double baseExpYieldW = random.nextDouble();
                poke.setCatchRate((int) Math.round(baseExpYieldW * (maxBaseExpYield - 1)) + 1);
            }
        }
        changesMade = true;
    }

    public void randomizeHeight() {
        if (settings.isRandomizeHeight()) {
            for (Species poke : romHandler.getSpeciesSetInclFormes()) {
                double heightW = random.nextDouble();
                poke.setHeight(((int) Math.round(heightW * (MAX_HEIGHT - 1)) + 1) * 10);
            }
        }
        changesMade = true;
    }

    public void randomizeWeight() {
        if (settings.isRandomizeWeight()) {
            for (Species poke : romHandler.getSpeciesSetInclFormes()) {
                double weightW = random.nextDouble();
                poke.setWeight((int) Math.round(weightW * (MAX_WEIGHT - 1)) + 1);
            }
        }
        changesMade = true;
    }

    public void standardizeEXPCurves() {
        Settings.ExpCurveMod mod = settings.getExpCurveMod();
        ExpCurve expCurve = settings.getSelectedEXPCurve();

        SpeciesSet pokes = romHandler.getSpeciesSetInclFormes();
        switch (mod) {
            case LEGENDARIES:
                for (Species pk : pokes) {
                    pk.setGrowthCurve(pk.isLegendary() ? ExpCurve.SLOW : expCurve);
                }
                break;
            case STRONG_LEGENDARIES:
                for (Species pk : pokes) {
                    pk.setGrowthCurve(pk.isStrongLegendary() ? ExpCurve.SLOW : expCurve);
                }
                break;
            case ALL:
                for (Species pk : pokes) {
                    pk.setGrowthCurve(expCurve);
                }
                break;
        }
    }

}
