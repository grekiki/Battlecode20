from playGame import play_game
from create_new_bots import *
import threading
import json

from collections import deque

MAPS = [
    'ALandDivided',
    'CentralLake',
    "CentralSoup",
    "ChristmasInJuly",
    "ClearlyTwelveHorsesInASalad",
    "CosmicBackgroundRadiation",
    "CowFarm",
    "DidAMonkeyMakeThis",
    "FourLakeLand",
    "GSF",
    "Hills",
    "InADitch",
    "Infinity",
    "IsThisProcedural",
    "Islands",
    "OmgThisIsProcedural",
    "ProceduralConfirmed",
    "RandomSoup1",
    "RandomSoup2",
    "Soup",
    "SoupOnTheSide",
    "TwoForOneAndTwoForAll",
    "Volcano",
    "WaterBot",
    "WateredDown",
]

class Referee(threading.Thread):

    def __init__(self, ids, q, results, generation):
        threading.Thread.__init__(self)
        self.id = ids
        self.q = q
        self.results = results
        self.generation = generation

    def run(self):
        
        while True:
            
            if len(self.q) == 0:
                break
            
            bot1, bot2, play_map = self.q.popleft()
            game_result = play_game(bot1, bot2, play_map)
            if game_result == 'PLAYER 1 WINS':
                self.results[bot1][bot2][0] += 1
                self.results[bot2][bot1][1] += 1
                print(f'GEN:{self.generation} - BOT {bot1} WINS VS BOT {bot2} ON MAP {play_map}')
            elif game_result == 'PLAYER 2 WINS':
                self.results[bot1][bot2][1] += 1
                self.results[bot2][bot1][0] += 1
                print(f'GEN:{self.generation} - BOT {bot2} WINS VS BOT {bot1} ON MAP {play_map}')
            else:
                print('There was an error in current game')
                print(bot1, bot2, play_map)


def play_tournament(generation, bot_names, referees=4):

    results = {}
    for bot1 in bot_names:
        results[bot1] = {}
        for bot2 in bot_names:
            results[bot1][bot2] = [0, 0] # Wins : loses
    
    matches = deque()
    for bot1 in bot_names:
        for bot2 in bot_names:
            if bot1 != bot2:
                for play_map in MAPS:
                    matches.append((bot1, bot2, play_map))
    
    referees_force = []
    for i in range(referees):
        referee = Referee(i, matches, results, generation)
        referee.start()
        referees_force.append(referee)
    
    for referee in referees_force:
        referee.join()

    print('TOURNAMENT IS OVER')
    
    with open(f'tournament_results/generation_{generation}.json', 'w') as outfile:
        json.dump(results, outfile, indent=2)
    
    with open(f'tournament_results/match_table_{generation}.txt', 'w') as outfile:
        standings = []

        for name in bot_names:

            total_wins = 0
            results_vs_opponents = []
            
            for opponent in bot_names:
                total_wins += results[name][opponent][0]
                results_vs_opponents.append(results[name][opponent][0])
            
            standings.append([total_wins, name] + results_vs_opponents)
        
        standings.sort(reverse=True)

        for result in standings:
            output_string = '{:<5}'.format(result[0])
            output_string += '{:<20} : '.format(result[1])
            for vs_opp in result[2:]:
                output_string += '{:<3} '.format(vs_opp)
            
            print(output_string, file=outfile)
        
        print('\n', file=outfile)
        for line in bot_names:
            print(line, file=outfile)
            
    for result in standings:
        print(result)
    
    return standings

def basic_evolution(bots, generation=4):

    while True:

        tournament_results = play_tournament(generation, bots, referees=5)

        next_generation = []

        bot_id = 0
        for parent in tournament_results[:5]:
            
            parent_bot = parent[1]
            next_generation.append(parent_bot) # Automatically advances as top5
            
            bot_name = f'generation_{generation}_id_{bot_id}'
            create_new_bot(parent_bot, generation, bot_id)
            next_generation.append(bot_name)
            
            bot_name = f'generation_{generation}_id_{bot_id+1}'
            create_new_bot(parent_bot, generation, bot_id+1)
            next_generation.append(bot_name)
            
            bot_id += 2

            bots = next_generation
        
        print('WE ARE ADVANCING TO NEXT GENERATION')
        generation += 1



if __name__ == '__main__':

    initial_bots = [
        'grekiki25_gen1',
        'generation_4_id_2',
        'generation_4_id_4',
        'generation_2_id_0',
        'generation_4_id_5',
        ]
    basic_evolution(initial_bots, generation=6)

    # generation = 3
    # bots = []
    # for i in range(5):
    #     bot_name = f'generation_{generation}_id_{i}'
    #     create_new_bot('grekiki25_gen1', generation, i)
    #     bots.append(bot_name)
    
    # bots.append('generation_0_id_0') # Previous winner
    # bots.append('grekiki25_gen1')
    
    # play_tournament(generation, bots, referees=10)
    

    # bot_1 = (0, 0)
    # # bot_2 = (0, 1)

    # # bot_1_name = f'generation_{bot_1[0]}_id_{bot_1[1]}'
    # # bot_2_name = f'generation_{bot_2[0]}_id_{bot_2[1]}'
    # bot_1_name = f'generation_{bot_1[0]}_id_{bot_1[1]}'
    # bot_2_name = 'grekiki25'
    
    # # create_new_bot('grekiki25', bot_1[0], bot_1[1])
    # # create_new_bot('grekiki25', bot_2[0], bot_2[1])

    # first_player_score = 0
    # second_player_score = 0

    # for play_map in MAPS:
    #     game = play_game(bot_1_name, bot_2_name, play_map)
    #     if game == 'PLAYER 1 WINS':
    #         first_player_score += 1
    #     elif game == 'PLAYER 2 WINS':
    #         second_player_score += 1
    #     else:
    #         print('ERROR!!')
    
    #     print(play_map, first_player_score, second_player_score)

