"""
Script that will create new bots. The procedure is the following:
- open json of some bot configurations in /bot_configurations
- modify values by a bit
- copy grekiki25 bot from Battlecode20/src into another bot folder
- change konst.java configuration into the configuration of current bot
"""

import subprocess
import json
import random

DOUBLES = set([
    "dist_optrange_factor",
    "dist_factor",
    "hqf3",
    "hqf4",   
])

def load_constants(bot_name):
    """
    Loads the configurations of the `bot_name` and returns it as a dictionary.
    """

    with open(f'bot_configurations/{bot_name}.json', 'r') as infile:
        data = json.load(infile)
    return data

def modify_constants(constants, percentages=10):
    """
    Modifies current configurations. So far it changes each parameter 
    from -10% up to 10%.

    Returns a dictionary of modified values.
    """

    for key, value in constants.items():
        if key == 'private_key':
            constants[key] = random.randint(10**8, 10**9)
        elif key in DOUBLES:
            change = random.random() / 5 - 0.1
            constants[key] = value * (1 + change)
        elif constants[key] < 10: # We allow small values to be updated
            constants[key] = random.randint(constants[key]-1, constants[key]+1)
        else:
            constants[key] = random.randint(round(value*0.9), round(value*1.1))

    return constants

def combine_two_parents(parent1_constants, parent2_constants):
    """
    Generate new bot constants by either averaging the two values or using one from one parent 
    or the other.
    """

    child_constants = {}
    for constant in parent1_constants:
        probability_action = random.random()
        if probability_action <= 1/3:
            child_constants[constant] = parent1_constants[constant]
        elif probability_action <= 2/3:
            child_constants[constant] = parent2_constants[constant]
        else:
            average = (parent1_constants[constant] + parent2_constants[constant]) / 2
            if constant not in DOUBLES:
                average = round(average)
            child_constants[constant] = average
    child_constants['private_key'] = random.randint(10**8, 10**9)

    return child_constants


def either_from_mother_or_father(parent1_constants, parent2_constants):
    """
    Generates new bot constants by either taking the value of parameter from one parent
    or from the other.
    """

    child_constants = {}
    for constant in parent1_constants:
        probability = random.random()
        if probability <= 0.5:
            child_constants[constant] = parent1_constants[constant]
        else:
            child_constants[constant] = parent2_constants[constant]
    child_constants['private_key'] = random.randint(10**8, 10**9)

    return child_constants



def create_new_bot(configuration, generation, number):
    """
    Creates a new bot out of the bot `parent`.
    Generation parameter specifies to which generation will this bot belong and 
    the number parameter is used to distuingish between bots within the same generation.

    - Modifies bot constants
    - saves them into file bot_configurations/generation_{generation}_id_{number}
    - copies contents of ../src/grekiki25 bot into a new folder named 
        ..\srs\generation_{generation}_id_{number}
    - modifies `konst.java` file and updates constants with updated ones
    """

    child_name = f'generation_{generation}_id_{number}'

    with open(f'bot_configurations/{child_name}.json', 'w') as outfile:
        json.dump(configuration, outfile, indent=2)

    command = subprocess.run(f'xcopy /s ..\src\grekiki25_ai ..\src\generation_{generation}_id_{number}\\ /Y', shell=True)

    with open(f'..\\src\\generation_{generation}_id_{number}\\konst.java', 'w') as outfile:
        # print(template.render(child_bot_configurations), file=outfile)
        print(f'package {child_name};', file=outfile)
        print('public class konst {', file=outfile)
        for key, value in configuration.items():
            if key in DOUBLES:
                print(f'public static double {key} = {value};', file=outfile)
            else:
                print(f'public static int {key} = {value};', file=outfile)
        print('}', file=outfile)
    
    # Change the package names in files
    for filename in ['buildings.java', 'pc.java', 'RobotPlayer.java', 'units.java']:
        with open(f'..\src\{child_name}\{filename}', 'r') as infile:
            lines = infile.readlines()
            lines[0] = f'package {child_name};'
        with open(f'..\src\{child_name}\{filename}', 'w') as outfile:
            for line in lines:
                print(line.rstrip('\n'), file=outfile)
        

if __name__ == '__main__':
    some_constants = load_constants('generation_7_id_1')
    create_new_bot(some_constants, 1, 12345567)