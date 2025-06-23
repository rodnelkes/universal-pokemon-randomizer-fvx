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
}
