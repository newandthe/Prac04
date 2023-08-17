package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleEnt {
//    private int took;
//    private boolean timed_out;
    private Hits hits;
    private int took;

    @JsonProperty("aggregations")
    private Aggregations aggregations;



    @Data
    public static class Hits { // art.getHits()
        private Total total;
//        private double max_score;
        private HitData[] hits;


        @Data
        public static class Total { // art.getHits.getTotal()
            private String value;   // art.getHits().getTotal().getValue() // 검색된 게시물의 개수
            private String relation;
        }

        @Data
        public static class HitData { //

            private double _score;
            private Source _source;
            private HitData.highlight highlight;

            @Data
            public static class Source {
                private String readcount;
                private String url_link;
                private String sub_title;
                private String author;
                private String file_name;
                private String title;       // art.getHits().getHits()[인덱스번호].get_source().getTitle()
                private String file_src;    // art.getHits().getHits()[0].get_source().getFile_src()
                private String nttId;
                private String content;
                private String domain;
                private String category;
                private String wdate;
                private String origin_name;
                private String file_hash;
                private _file[] _file;

                @Data
                public static class _file {
                    private String reason;
                    private String path;
                    private String extension;
                    private String preView;
                    private String name;
                    private String mediaType;
                    private String nameOrg;
                    private String content;
                }
            }


            @Data
            public static class highlight {
                private String[] title;

                private String[] content;

                @JsonProperty("_file.content")
                private String[] fileContentHighlight;

                @JsonProperty("_file.nameOrg")
                private String[] filenameOrgHighlight;
            }


        }




    }


    @Data
    public static class Aggregations {

        @JsonProperty("sterms#categories")
        private Categories categories;

        @Data
        public static class Categories {
            private int doc_count_error_upper_bound;
            private int sum_other_doc_count;
            private Bucket[] buckets;
        }

        @Data
        public static class Bucket {
            private String key;
            private int doc_count;

            @JsonProperty("top_hits#top_documents")
            private TopDocuments top_documents;
        }

        @Data
        public static class TopDocuments {
            private Hits hits;
        }

        @Data
        public static class Hits {
            private Total total;
            private HitData[] hits;

            @Data
            public static class Total {
                private int value;
                private String relation;
            }

            @Data
            public static class HitData {
                private double _score;
                private Source _source;
            }

            private double _score;

            @JsonProperty("_source")
            private Source _source;

            @Data
            public static class Source {
                private String readcount;
                private String url_link;
                private String sub_title;
                private String author;
                private String file_name;
                private String title;
                private String file_src;
                private String nttId;
                private String content;
                private String domain;
                private String category;
                private String wdate;
                private String origin_name;
                private String file_hash;
                private _file[] _file;

                @Data
                public static class _file {
                    private String reason;
                    private String path;
                    private String extension;
                    private String preView;
                    private String name;
                    private String mediaType;
                    private String nameOrg;
                    private String content;
                }
            }
        }
    }


}
