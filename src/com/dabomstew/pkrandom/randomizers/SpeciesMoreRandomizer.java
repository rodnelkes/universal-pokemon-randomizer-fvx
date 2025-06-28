package com.dabomstew.pkrandom.randomizers;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.gamedata.Species;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

import java.util.Random;

public class SpeciesMoreRandomizer extends Randomizer{

    public SpeciesMoreRandomizer(RomHandler romHandler, Settings settings, Random random) {
        super(romHandler, settings, random);
    }

    public void randomizeCatchRate() {
        if (settings.isRandomizeCatchRate()) {
            for (Species poke : romHandler.getSpeciesInclFormes()) {
                if (poke != null)
                    poke.randomizeCatchRate(random);
            }
        }
        changesMade = true;
    }

    public void randomizeEvYields() {
        if (settings.isRandomizeEVYields()) {
            for (Species poke : romHandler.getSpeciesInclFormes()) {
                if (poke != null)
                    poke.randomizeEvYields(random);
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
                maxBaseExpYield = 608;
            else
                throw new IllegalStateException("Must implement for newer generation: " + generation);

            for (Species poke : romHandler.getSpeciesInclFormes()) {
                if (poke != null)
                    poke.randomizeBaseExpYield(random, maxBaseExpYield);
            }
        }
        changesMade = true;
    }
}
