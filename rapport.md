# Rapport TP2 GRE
### Camille Theubet, Léonard Jouve

## Commentaires à propos des résultats obtenus

Pour les 4 heuristiques optimistes, il était demandé d'afficher respectivement 
la moyenne de la longueur du chemin trouvé, le nombre moyen de sommets traités,
ainsi que la différence moyenne (en %) de sommets traités par rapport à l'heuristique de Dijkstra.

Concernant la longueur du chemin trouvé, ces heuristiques étants optimistes, et permettant donc de toujours trouver le plus court chemin,
nous nous attendions à trouver toujours
la même longueur moyenne, soit celle du chemin le plus court entre la source et la destination.
Ce fût le cas pour les 3 premières expériences. Mais pourtant durant la 4ème et 5ème expérience, nous obtenons une moyenne 
très légèrement différente entre les heuristiques. La 6ème expérience souffre également d'une différence de l'ordre de quelques
millièmes d'unités. Ces erreurs n'étants pas censées apparaître, nous supposons que notre code contient une erreur, ou au moins
une erreur dans la gestion d'un overflow, ou d'un calcul de précision. 

Concernant le nombre moyen de sommets traités, nous nous attendions cette fois à des valeurs différentes, tout l'intérêt
du choix de l'heuristique reposant justement en partie sur le nombre de sommets qui seront traités. Et effectivement, 
nous obtenons des valeurs différentes. Parmis les 4 heuristiques optimistes, Dijkstra est toujours la pire, et parfois de très loin.
Mais cet écart s'atténue avec l'augmentation du niveau de densité et la réduction du niveau d'ouverture du graphe sur lequel on cherche le plus court
chemin. Cela se voit particulièrement bien quand on regarde le % de la diminution du nombre de sommets traités par rapport à Dijkstra.

On constate également que le changement vers une pondération forte durant la 6ème expérience augmente considérablement
le nombre de sommets traités par rapport à la 5ème qui a les mêmes paramètres d'agissant de la densité et de l'ouverture du labyrinthe.
Cette augmentation des sommets traités semble également atténuer le gain de performance que fournit AStar sur Dijkstra, 
car le pourcentage de baisse du nombre de sommets traités est de plus en plus faible.

## K-Manhattan

L'algorithme de K-Manhattan n'est pas optimiste, ce qui signifie qu'il ne trouve pas obligatoirement le chemin le plus court jusqu'à 
la destination, mais un chemin quelconque. Le fait qu'une heuristique soit dite optimiste est dû au fait qu'elle estime ou 
non une distance toujours inférieure ou égale à la distance réelle entre 2 points.
Celà est clairement le cas de l'algorithme de Manhattan par exemple, car aucun chemin ne peut être plus court que le chemin direct.
Ce n'est par contre pas le cas de K-Manhattan qui multiplie cette distance par une constante arbitraire et ne peut ainsi 
pas garantir que la distance prédite est inférieure ou égale à la distance réelle entre les 2 points. La constance K va 
accentuer la tendance à favoriser les sommets se rapprochant de la destination. Les points situés loins de la destination
sont alors plus fortement penalisés. On remarque dans notre exemple que plus la valeur de K est élevée, plus le gain de sommets traités par rapport à Dijkstra est important.
Celà se remarque d'autant plus dans des graphes denses. Par contre, l'erreur moyenne de longueur du chemin trouvé augmente 
également avec la valeur de K choisie. Il est alors intéressant de trouver un compromis entre performance de l'algorithme 
et exactitude du plus court chemin en fonction du besoin.
