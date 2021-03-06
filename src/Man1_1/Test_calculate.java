package Man1_1;

import HModel.Column_ian;
import SA.Unify_new;
import common.Constant;
import queries.QueryPicture;
import replicas.AckSeq;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/*
157.95s vs 441.45
 */
public class Test_calculate{
        public static void main(String[] args) {
            // 数据分布参数
            BigDecimal totalRowNumber = new BigDecimal("40000000");

            List<Column_ian> CKdist = new ArrayList<Column_ian>();
            double step = 1;
            List<Double> x = new ArrayList<Double>();
            for (int i = 1; i <= 101; i++) {
                x.add((double) i);
            }
            List<Integer> y = new ArrayList<Integer>();
            for (int i = 1; i <= 100; i++) {
                y.add(1);
            }
            Column_ian ck1 = new Column_ian(step, x, y);
            Column_ian ck2 = new Column_ian(step, x, y);
            Column_ian ck3 = new Column_ian(step, x, y);
            Column_ian ck4 = new Column_ian(step, x, y);
            Column_ian ck5 = new Column_ian(step, x, y);
            Column_ian ck6 = new Column_ian(step, x, y);
            Column_ian ck7 = new Column_ian(step, x, y);
            Column_ian ck8 = new Column_ian(step, x, y);
            Column_ian ck9 = new Column_ian(step, x, y);
            Column_ian ck10 = new Column_ian(step, x, y);
            CKdist.add(ck1);
            CKdist.add(ck2);
            CKdist.add(ck3);
            CKdist.add(ck4);
            CKdist.add(ck5);
            CKdist.add(ck6);
            CKdist.add(ck7);
            CKdist.add(ck8);
            CKdist.add(ck9);
            CKdist.add(ck10);

            int ckn=CKdist.size();

            // 数据存储参数
            int rowSize = 52;
            int fetchRowCnt = 100;
            double costModel_k = 2.34;
            double costModel_b = 18927.55;
            double cost_session_around = 242.79;
            double cost_request_around = 808.42;

            // 查询参数
            double[][] starts = new double[ckn][];
            double[][] lengths = new double[ckn][];
            int[] qpernum = new int[]{21,12,9,5,4,3,2,2,1,1};

            for(int i=0;i<ckn;i++){
                starts[i] = new double[]{0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1,0.1};
                lengths[i] = new double[]{0.08,0.2,0.28,0.16,0.12,0.04,0.04,0.04,0.04,0};
            }
            QueryPicture queryPicture = new QueryPicture(starts,lengths,qpernum,15);

            int X = 3;
            Unify_new unify_new = new Unify_new(totalRowNumber,
                    ckn, CKdist,
                    rowSize, fetchRowCnt, costModel_k, costModel_b, cost_session_around, cost_request_around,
                    queryPicture,
                    X);

            unify_new.isDiffReplicated = false;
            unify_new.calculate_unit(new AckSeq[]{new AckSeq(new int[]{6,3,7,10,9,4,5,2,1,8}),
                    new AckSeq(new int[]{6,3,7,10,9,4,5,2,1,8}),
                    new AckSeq(new int[]{6,3,7,10,9,4,5,2,1,8})
            });

//            unify_new.isDiffReplicated = true;
//            unify_new.calculate_unit(new AckSeq[]{new AckSeq(new int[]{5,8,6,9,2,10,3,1,4,7}),
//                    new AckSeq(new int[]{10,5,6,2,8,3,1,4,7,9}), // 10,5,6,2,8,3,1,4,7,9
//                    new AckSeq(new int[]{9,8,3,4,1,7,10,6,5,2}) //9,8,3,4,1,7,10,6,5,2
//            });//5,8,6,9,2,10,3,1,4,7

//            unify_new.isDiffReplicated = false;
//            unify_new.calculate_unit(new AckSeq[]{new AckSeq(new int[]{10,9,8,7,6,5,4,3,2,1}),
//                    new AckSeq(new int[]{10,9,8,7,6,5,4,3,2,1}), // 10,5,6,2,8,3,1,4,7,9
//                    new AckSeq(new int[]{10,9,8,7,6,5,4,3,2,1}) //9,8,3,4,1,7,10,6,5,2
//            });//5,8,6,9,2,10,3,1,4,7
//            unify_new.calculate_unit(new AckSeq[]{new AckSeq(new int[]{9,10,8,7,6,5,4,2,3,1}),
//                    new AckSeq(new int[]{9,10,8,7,6,5,4,2,3,1}), // 10,5,6,2,8,3,1,4,7,9
//                    new AckSeq(new int[]{9,10,8,7,6,5,4,2,3,1}) //9,8,3,4,1,7,10,6,5,2
//            });//5,8,6,9,2,10,3,1,4,7


        }
}
