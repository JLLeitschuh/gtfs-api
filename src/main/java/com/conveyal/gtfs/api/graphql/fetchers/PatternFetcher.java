package com.conveyal.gtfs.api.graphql.fetchers;

import com.conveyal.gtfs.api.ApiMain;
import com.conveyal.gtfs.api.graphql.WrappedGTFSEntity;
import com.conveyal.gtfs.api.models.FeedSource;
import com.conveyal.gtfs.api.util.GeomUtil;
import com.conveyal.gtfs.model.Pattern;
import com.conveyal.gtfs.model.Route;
import com.conveyal.gtfs.model.Trip;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import graphql.schema.DataFetchingEnvironment;

import java.util.*;
import java.util.stream.Collectors;

import static com.conveyal.gtfs.api.util.GraphQLUtil.argumentDefined;

/**
 *
 * Created by matthewc on 3/9/16.
 */
public class PatternFetcher {
    private static final Double DEFAULT_RADIUS = 1.0; // default 1 km search radius

    public static List<WrappedGTFSEntity<Pattern>> apex(DataFetchingEnvironment env) {
        Collection<FeedSource> feeds;

        List<String> feedId = (List<String>) env.getArgument("feed_id");
        feeds = ApiMain.getFeedSources(feedId);
        Map<String, Object> args = env.getArguments();
        List<WrappedGTFSEntity<Pattern>> patterns = new ArrayList<>();

        for (FeedSource fs : feeds) {
            if (env.getArgument("pattern_id") != null) {
                List<String> patternId = (List<String>) env.getArgument("pattern_id");
                patternId.stream()
                        .filter(fs.feed.patterns::containsKey)
                        .map(fs.feed.patterns::get)
                        .map(pattern -> new WrappedGTFSEntity(fs.id, pattern))
                        .forEach(patterns::add);
            }
            else if (env.getArgument("route_id") != null) {
                List<String> routeId = (List<String>) env.getArgument("route_id");
                fs.feed.patterns.values().stream()
                        .filter(p -> routeId.contains(p.route_id))
                        .map(pattern -> new WrappedGTFSEntity(fs.id, pattern))
                        .forEach(patterns::add);
            }
        }

        return patterns;
    }
    public static List<WrappedGTFSEntity<Pattern>> fromRoute(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Route> route = (WrappedGTFSEntity<Route>) env.getSource();
        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(route.feedUniqueId);
        if (fs == null) return null;

        List<String> stopIds = env.getArgument("stop_id");
        List<String> patternId = env.getArgument("pattern_id");
        Long limit = env.getArgument("limit");

        List<WrappedGTFSEntity<Pattern>> patterns = fs.feed.patterns.values().stream()
                .filter(p -> p.route_id.equals(route.entity.route_id))
                .map(p -> new WrappedGTFSEntity<>(fs.id, p))
                .collect(Collectors.toList());
        if (patternId != null) {
            patterns.stream()
                    .filter(p -> patternId.contains(p.entity.pattern_id))
                    .collect(Collectors.toList());
        }
        if (stopIds != null) {
            patterns.stream()
                    .filter(p -> !Collections.disjoint(p.entity.orderedStops, stopIds)) // disjoint returns true if no elements in common
                    .collect(Collectors.toList());
        }
        if (limit != null) {
            return patterns.stream().limit(limit).collect(Collectors.toList());
        }
        return patterns;
    }

    public static Long fromRouteCount(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Route> route = (WrappedGTFSEntity<Route>) env.getSource();
        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(route.feedUniqueId);
        if (fs == null) return null;

        return fs.feed.patterns.values().stream()
                .filter(p -> p.route_id.equals(route.entity.route_id))
                .count();
    }

    public static WrappedGTFSEntity<Pattern> fromTrip(DataFetchingEnvironment env) {
        WrappedGTFSEntity<Trip> trip = (WrappedGTFSEntity<Trip>) env.getSource();
        FeedSource fs = ApiMain.getFeedSourceWithoutExceptions(trip.feedUniqueId);
        if (fs == null) return null;

        Pattern patt = fs.feed.patterns.get(fs.feed.tripPatternMap.get(trip.entity.trip_id));
        return new WrappedGTFSEntity<>(fs.id, patt);
    }
}
